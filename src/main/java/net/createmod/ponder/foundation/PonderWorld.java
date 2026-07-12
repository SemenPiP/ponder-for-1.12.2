package net.createmod.ponder.foundation;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraft.world.gen.structure.template.TemplateManager;
import net.minecraft.world.storage.IPlayerFileData;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldInfo;
import net.createmod.ponder.api.scene.Selection;

/**
 * An entirely in-memory world for scene playback. Coordinates stored by this world are scene-local.
 */
public class PonderWorld extends World {
    private static final Logger LOGGER = LogManager.getLogger("PonderWorld");
    private static final int MAX_PARTICLES = 4096;
    private static final String SNAPSHOT_ITEM_HOVER_START = "PonderItemHoverStart";

    private final BlockPos anchor;
    @Nullable
    private final World originalWorld;
    private final Map<BlockPos, Boolean> occupiedPositions = new LinkedHashMap<BlockPos, Boolean>();
    private final Map<Integer, BreakProgress> blockBreakingProgress = new LinkedHashMap<Integer, BreakProgress>();
    private final List<ParticleEvent> particles = new ArrayList<ParticleEvent>();
    private MemoryChunkProvider memoryChunkProvider;
    @Nullable
    private Snapshot backup;
    private int nextEntityId = 1;
    private long stateVersion;

    public PonderWorld(BlockPos anchor, @Nullable World originalWorld) {
        super(MemorySaveHandler.INSTANCE, createWorldInfo(), new WorldProviderSurface(), new Profiler(), true);
        this.anchor = anchor == null ? BlockPos.ORIGIN : anchor.toImmutable();
        this.originalWorld = originalWorld;
        provider.setWorld(this);
        worldInfo.setDifficulty(EnumDifficulty.PEACEFUL);
        chunkProvider = createChunkProvider();
        mapStorage = new MapStorage(null);
        setSpawnPoint(BlockPos.ORIGIN);
    }

    private static WorldInfo createWorldInfo() {
        WorldSettings settings = new WorldSettings(0L, GameType.CREATIVE, false, false, WorldType.FLAT);
        return new WorldInfo(settings, "Ponder");
    }

    public BlockPos getAnchor() {
        return anchor;
    }

    @Nullable
    public World getOriginalWorld() {
        return originalWorld;
    }

    public BlockPos localToWorld(BlockPos local) {
        return anchor.add(local);
    }

    public BlockPos worldToLocal(BlockPos global) {
        return global.subtract(anchor);
    }

    @Override
    protected IChunkProvider createChunkProvider() {
        memoryChunkProvider = new MemoryChunkProvider(this);
        return memoryChunkProvider;
    }

    @Override
    protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) {
        return true;
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        if (isOutsideBuildHeight(pos)) {
            return Blocks.AIR.getDefaultState();
        }
        return getChunk(pos).getBlockState(pos);
    }

    @Override
    public boolean setBlockState(BlockPos pos, IBlockState newState, int flags) {
        if (newState == null || isOutsideBuildHeight(pos)) {
            return false;
        }
        BlockPos immutable = pos.toImmutable();
        IBlockState oldState = getBlockState(immutable);
        if (oldState == newState || oldState.equals(newState)) {
            return false;
        }
        IBlockState replaced = getChunk(immutable).setBlockState(immutable, newState);
        if (replaced == null) {
            return false;
        }
        if (newState.getBlock() == Blocks.AIR) {
            occupiedPositions.remove(immutable);
            removeTileEntity(immutable);
        } else {
            occupiedPositions.put(immutable, Boolean.TRUE);
        }
        return true;
    }

    @Override
    @Nullable
    public TileEntity getTileEntity(BlockPos pos) {
        if (isOutsideBuildHeight(pos)) {
            return null;
        }
        return getChunk(pos).getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);
    }

    @Override
    public void setTileEntity(BlockPos pos, @Nullable TileEntity tileEntity) {
        if (tileEntity == null || isOutsideBuildHeight(pos)) {
            return;
        }
        BlockPos immutable = pos.toImmutable();
        TileEntity old = getTileEntity(immutable);
        if (old != null) {
            loadedTileEntityList.remove(old);
            tickableTileEntities.remove(old);
            old.invalidate();
        }
        tileEntity.setWorld(this);
        tileEntity.setPos(immutable);
        tileEntity.validate();
        getChunk(immutable).addTileEntity(immutable, tileEntity);
        if (!loadedTileEntityList.contains(tileEntity)) {
            loadedTileEntityList.add(tileEntity);
        }
        if (tileEntity instanceof ITickable && !tickableTileEntities.contains(tileEntity)) {
            tickableTileEntities.add(tileEntity);
        }
    }

    @Override
    public void removeTileEntity(BlockPos pos) {
        TileEntity tileEntity = getTileEntity(pos);
        if (tileEntity != null) {
            loadedTileEntityList.remove(tileEntity);
            tickableTileEntities.remove(tileEntity);
            tileEntity.invalidate();
        }
        getChunk(pos).removeTileEntity(pos);
    }

    public Collection<TileEntity> getTileEntities() {
        return Collections.unmodifiableList(new ArrayList<TileEntity>(loadedTileEntityList));
    }

    public Collection<BlockPos> getOccupiedPositions() {
        return Collections.unmodifiableList(new ArrayList<BlockPos>(occupiedPositions.keySet()));
    }

    public BlockPos getBoundsMin() {
        if (occupiedPositions.isEmpty()) {
            return BlockPos.ORIGIN;
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        for (BlockPos pos : occupiedPositions.keySet()) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
        }
        return new BlockPos(minX, minY, minZ);
    }

    public BlockPos getBoundsMax() {
        if (occupiedPositions.isEmpty()) {
            return BlockPos.ORIGIN;
        }
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : occupiedPositions.keySet()) {
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        return new BlockPos(maxX, maxY, maxZ);
    }

    public Collection<Entity> getEntities() {
        return Collections.unmodifiableList(new ArrayList<Entity>(loadedEntityList));
    }

    @Override
    public boolean spawnEntity(Entity entity) {
        if (entity == null || loadedEntityList.contains(entity)) {
            return false;
        }
        entity.setWorld(this);
        int requestedId = entity.getEntityId();
        if (requestedId <= 0 || getEntityByID(requestedId) != null) {
            while (getEntityByID(nextEntityId) != null) nextEntityId++;
            entity.setEntityId(nextEntityId++);
        } else {
            nextEntityId = Math.max(nextEntityId, requestedId + 1);
        }
        loadedEntityList.add(entity);
        entitiesById.addKey(entity.getEntityId(), entity);
        if (entity instanceof EntityPlayer) {
            playerEntities.add((EntityPlayer) entity);
        }
        return true;
    }

    @Override
    public void removeEntity(Entity entity) {
        if (entity == null) {
            return;
        }
        loadedEntityList.remove(entity);
        entitiesById.removeObject(entity.getEntityId());
        playerEntities.remove(entity);
        entity.setDead();
    }

    @Override
    public void spawnParticle(EnumParticleTypes type, double x, double y, double z,
                              double velocityX, double velocityY, double velocityZ, int... parameters) {
        addParticle(new ParticleEvent(type, x, y, z, velocityX, velocityY, velocityZ, parameters));
    }

    @Override
    public void spawnParticle(EnumParticleTypes type, boolean ignoreRange, double x, double y, double z,
                              double velocityX, double velocityY, double velocityZ, int... parameters) {
        addParticle(new ParticleEvent(type, x, y, z, velocityX, velocityY, velocityZ, parameters));
    }

    private void addParticle(ParticleEvent event) {
        if (particles.size() == MAX_PARTICLES) {
            particles.remove(0);
        }
        particles.add(event);
    }

    public List<ParticleEvent> drainParticles() {
        List<ParticleEvent> result = new ArrayList<ParticleEvent>(particles);
        particles.clear();
        return result;
    }

    @Override
    public void sendBlockBreakProgress(int breakerId, BlockPos pos, int progress) {
        setBlockBreakingProgress(breakerId, pos, progress);
    }

    public void setBlockBreakingProgress(int breakerId, BlockPos pos, int progress) {
        if (progress < 0 || progress > 9) {
            blockBreakingProgress.remove(breakerId);
        } else {
            blockBreakingProgress.put(breakerId, new BreakProgress(pos.toImmutable(), progress));
        }
    }

    public Map<Integer, BreakProgress> getBlockBreakingProgress() {
        return Collections.unmodifiableMap(blockBreakingProgress);
    }

    @Override
    public int getLightFor(EnumSkyBlock type, BlockPos pos) {
        return type == EnumSkyBlock.SKY ? 15 : Math.max(0,
            Math.min(15, getBlockState(pos).getLightValue(this, pos)));
    }

    @Override
    public int getCombinedLight(BlockPos pos, int minimumBlockLight) {
        int block = Math.max(minimumBlockLight, getLightFor(EnumSkyBlock.BLOCK, pos));
        return 15 << 20 | block << 4;
    }

    public void tickVirtualWorld() {
        setTotalWorldTime(getTotalWorldTime() + 1L);
        setWorldTime(getWorldTime() + 1L);
        for (TileEntity tile : new ArrayList<TileEntity>(tickableTileEntities)) {
            if (!tile.isInvalid() && tile instanceof ITickable) {
                try {
                    ((ITickable) tile).update();
                } catch (Throwable throwable) {
                    LOGGER.error("Tile renderer state update failed at {}", tile.getPos(), throwable);
                }
            }
        }
        for (Entity entity : new ArrayList<Entity>(loadedEntityList)) {
            if (entity.isDead) {
                loadedEntityList.remove(entity);
                entitiesById.removeObject(entity.getEntityId());
                continue;
            }
            entity.lastTickPosX = entity.posX;
            entity.lastTickPosY = entity.posY;
            entity.lastTickPosZ = entity.posZ;
            entity.prevRotationYaw = entity.rotationYaw;
            entity.prevRotationPitch = entity.rotationPitch;
            try {
                entity.onUpdate();
            } catch (Throwable throwable) {
                LOGGER.error("Entity update failed for {}", entity, throwable);
            }
        }
    }

    public void backup() {
        backup = createSnapshot();
    }

    public void backupBlocks() {
        backup();
    }

    public void restore() {
        if (backup != null) {
            restoreSnapshot(backup);
        }
    }

    public void restoreBlocks() {
        restore();
    }

    public void restoreBlocks(Selection selection) {
        if (backup == null || selection == null || selection.isEmpty()) {
            return;
        }
        Map<BlockPos, NBTTagCompound> backedUpTiles = new HashMap<BlockPos, NBTTagCompound>();
        for (NBTTagCompound tag : backup.tileData) {
            backedUpTiles.put(new BlockPos(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z")), tag);
        }
        for (BlockPos pos : selection) {
            BlockPos immutable = pos.toImmutable();
            removeTileEntity(immutable);
            IBlockState state = backup.blocks.get(immutable);
            setBlockState(immutable, state == null ? Blocks.AIR.getDefaultState() : state, 0);
            NBTTagCompound tileData = backedUpTiles.get(immutable);
            if (tileData != null) {
                TileEntity tileEntity = TileEntity.create(this, tileData.copy());
                if (tileEntity != null) {
                    setTileEntity(immutable, tileEntity);
                }
            }
        }
    }

    public Snapshot createSnapshot() {
        Map<BlockPos, IBlockState> blocks = new LinkedHashMap<BlockPos, IBlockState>();
        for (BlockPos pos : occupiedPositions.keySet()) {
            blocks.put(pos, getBlockState(pos));
        }
        List<NBTTagCompound> tileData = new ArrayList<NBTTagCompound>();
        for (TileEntity tileEntity : loadedTileEntityList) {
            tileData.add(tileEntity.writeToNBT(new NBTTagCompound()));
        }
        List<NBTTagCompound> entityData = new ArrayList<NBTTagCompound>();
        for (Entity entity : loadedEntityList) {
            NBTTagCompound tag = new NBTTagCompound();
            if (entity.writeToNBTAtomically(tag)) {
                if (entity instanceof EntityItem) {
                    tag.setFloat(SNAPSHOT_ITEM_HOVER_START, ((EntityItem) entity).hoverStart);
                }
                entityData.add(tag);
            }
        }
        return new Snapshot(blocks, tileData, entityData,
            new LinkedHashMap<Integer, BreakProgress>(blockBreakingProgress), new ArrayList<ParticleEvent>(particles),
            getTotalWorldTime(), getWorldTime());
    }

    public void restoreSnapshot(Snapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot");
        }
        clear();
        for (Map.Entry<BlockPos, IBlockState> entry : snapshot.blocks.entrySet()) {
            setBlockState(entry.getKey(), entry.getValue(), 0);
        }
        for (NBTTagCompound data : snapshot.tileData) {
            TileEntity tileEntity = TileEntity.create(this, data.copy());
            if (tileEntity != null) {
                setTileEntity(tileEntity.getPos(), tileEntity);
            }
        }
        for (NBTTagCompound data : snapshot.entityData) {
            NBTTagCompound entityData = data.copy();
            boolean hasHoverStart = entityData.hasKey(SNAPSHOT_ITEM_HOVER_START, 5);
            float hoverStart = entityData.getFloat(SNAPSHOT_ITEM_HOVER_START);
            entityData.removeTag(SNAPSHOT_ITEM_HOVER_START);
            Entity entity = EntityList.createEntityFromNBT(entityData, this);
            if (entity != null) {
                if (hasHoverStart && entity instanceof EntityItem) {
                    ((EntityItem) entity).hoverStart = hoverStart;
                }
                spawnEntity(entity);
            }
        }
        blockBreakingProgress.putAll(snapshot.breakingProgress);
        particles.addAll(snapshot.particles);
        setTotalWorldTime(snapshot.totalWorldTime);
        setWorldTime(snapshot.worldTime);
        stateVersion++;
    }

    public void clear() {
        for (TileEntity tile : loadedTileEntityList) {
            tile.invalidate();
        }
        for (Entity entity : loadedEntityList) {
            entity.setDead();
        }
        loadedTileEntityList.clear();
        tickableTileEntities.clear();
        loadedEntityList.clear();
        playerEntities.clear();
        occupiedPositions.clear();
        blockBreakingProgress.clear();
        particles.clear();
        entitiesById.clearMap();
        memoryChunkProvider.clear();
        nextEntityId = 1;
    }

    public static final class ParticleEvent {
        public final EnumParticleTypes type;
        public final double x;
        public final double y;
        public final double z;
        public final double velocityX;
        public final double velocityY;
        public final double velocityZ;
        public final int[] parameters;

        private ParticleEvent(EnumParticleTypes type, double x, double y, double z,
                              double velocityX, double velocityY, double velocityZ, int[] parameters) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.z = z;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.velocityZ = velocityZ;
            this.parameters = parameters == null ? new int[0] : parameters.clone();
        }
    }

    public static final class BreakProgress {
        public final BlockPos pos;
        public final int progress;

        private BreakProgress(BlockPos pos, int progress) {
            this.pos = pos;
            this.progress = progress;
        }
    }

    public static final class Snapshot {
        private final Map<BlockPos, IBlockState> blocks;
        private final List<NBTTagCompound> tileData;
        private final List<NBTTagCompound> entityData;
        private final Map<Integer, BreakProgress> breakingProgress;
        private final List<ParticleEvent> particles;
        private final long totalWorldTime;
        private final long worldTime;

        private Snapshot(Map<BlockPos, IBlockState> blocks, List<NBTTagCompound> tileData,
                         List<NBTTagCompound> entityData, Map<Integer, BreakProgress> breakingProgress,
                         List<ParticleEvent> particles,
                         long totalWorldTime, long worldTime) {
            this.blocks = Collections.unmodifiableMap(new LinkedHashMap<BlockPos, IBlockState>(blocks));
            this.tileData = copyTags(tileData);
            this.entityData = copyTags(entityData);
            this.breakingProgress = Collections.unmodifiableMap(
                new LinkedHashMap<Integer, BreakProgress>(breakingProgress));
            this.particles = Collections.unmodifiableList(new ArrayList<ParticleEvent>(particles));
            this.totalWorldTime = totalWorldTime;
            this.worldTime = worldTime;
        }

        private static List<NBTTagCompound> copyTags(List<NBTTagCompound> source) {
            List<NBTTagCompound> copy = new ArrayList<NBTTagCompound>(source.size());
            for (NBTTagCompound tag : source) {
                copy.add(tag.copy());
            }
            return Collections.unmodifiableList(copy);
        }
    }

    public long getStateVersion() {
        return stateVersion;
    }

    private static final class MemoryChunkProvider implements IChunkProvider {
        private final PonderWorld world;
        private final Map<Long, Chunk> chunks = new HashMap<Long, Chunk>();

        private MemoryChunkProvider(PonderWorld world) {
            this.world = world;
        }

        @Override
        @Nullable
        public Chunk getLoadedChunk(int x, int z) {
            return chunks.get(key(x, z));
        }

        @Override
        public Chunk provideChunk(int x, int z) {
            long key = key(x, z);
            Chunk chunk = chunks.get(key);
            if (chunk == null) {
                chunk = new Chunk(world, x, z);
                chunk.setTerrainPopulated(true);
                chunk.setLightPopulated(true);
                chunks.put(key, chunk);
            }
            return chunk;
        }

        @Override
        public boolean tick() {
            return false;
        }

        @Override
        public String makeString() {
            return "PonderMemoryChunkProvider[" + chunks.size() + "]";
        }

        @Override
        public boolean isChunkGeneratedAt(int x, int z) {
            return true;
        }

        private void clear() {
            chunks.clear();
        }

        private static long key(int x, int z) {
            return (x & 0xffffffffL) | (z & 0xffffffffL) << 32;
        }
    }

    private static final class MemorySaveHandler implements ISaveHandler {
        private static final MemorySaveHandler INSTANCE = new MemorySaveHandler();
        @Override @Nullable public WorldInfo loadWorldInfo() { return null; }
        @Override public void checkSessionLock() throws MinecraftException {}
        @Override @Nullable public IChunkLoader getChunkLoader(WorldProvider provider) { return null; }
        @Override public void saveWorldInfoWithPlayer(WorldInfo info, NBTTagCompound playerTag) {}
        @Override public void saveWorldInfo(WorldInfo info) {}
        @Override @Nullable public IPlayerFileData getPlayerNBTManager() { return null; }
        @Override public void flush() {}
        @Override @Nullable public File getWorldDirectory() { return null; }
        @Override @Nullable public File getMapFileFromName(String mapName) { return null; }
        @Override @Nullable public TemplateManager getStructureTemplateManager() { return null; }
    }
}
