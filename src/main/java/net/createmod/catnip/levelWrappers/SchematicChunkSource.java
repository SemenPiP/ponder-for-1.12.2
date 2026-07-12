package net.createmod.catnip.levelWrappers;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;

/** Chunk views backed directly by a {@link SchematicLevel}. */
public class SchematicChunkSource implements IChunkProvider {
    private final SchematicLevel schematic;
    private final World fallbackWorld;
    private final Map<Long, Chunk> chunks = new LinkedHashMap<Long, Chunk>();

    public SchematicChunkSource(SchematicLevel schematic) {
        if (schematic == null) throw new IllegalArgumentException("schematic");
        World world = schematic.getWorld();
        if (world == null)
            throw new IllegalArgumentException("SchematicChunkSource needs a World-backed SchematicLevel");
        this.schematic = schematic;
        fallbackWorld = world;
    }

    @Nullable
    @Override
    public synchronized Chunk getLoadedChunk(int x, int z) {
        return chunks.get(key(x, z));
    }

    @Override
    public synchronized Chunk provideChunk(int x, int z) {
        long key = key(x, z);
        Chunk chunk = chunks.get(key);
        if (chunk == null) {
            chunk = new SchematicChunk(fallbackWorld, x, z, schematic);
            chunk.setTerrainPopulated(true);
            chunk.setLightPopulated(true);
            chunks.put(key, chunk);
        }
        return chunk;
    }

    @Override public boolean tick() { return false; }
    @Override public synchronized String makeString() { return "SchematicChunkSource[" + chunks.size() + "]"; }
    @Override public boolean isChunkGeneratedAt(int x, int z) { return true; }
    public synchronized void clear() { chunks.clear(); }

    private static long key(int x, int z) { return x & 0xffffffffL | (z & 0xffffffffL) << 32; }

    public static class SchematicChunk extends Chunk {
        private final int chunkX;
        private final int chunkZ;
        private final SchematicLevel schematic;

        public SchematicChunk(World world, int chunkX, int chunkZ, SchematicLevel schematic) {
            super(world, chunkX, chunkZ);
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.schematic = schematic;
        }

        @Override public boolean isAtLocation(int x, int z) { return x == chunkX && z == chunkZ; }
        @Override public IBlockState getBlockState(BlockPos pos) { return schematic.getBlockState(pos); }
        @Override public IBlockState setBlockState(BlockPos pos, IBlockState state) {
            IBlockState previous = schematic.getBlockState(pos);
            schematic.setBlockState(pos, state, 0);
            return previous;
        }
        @Nullable @Override public TileEntity getTileEntity(BlockPos pos, EnumCreateEntityType creationMode) {
            return schematic.getTileEntity(pos);
        }
        @Override public void addTileEntity(TileEntity tileEntity) {
            if (tileEntity == null) throw new IllegalArgumentException("tileEntity");
            schematic.setTileEntity(tileEntity.getPos(), tileEntity);
        }
        @Override public void addTileEntity(BlockPos pos, TileEntity tileEntity) {
            schematic.setTileEntity(pos, tileEntity);
        }
        @Override public void removeTileEntity(BlockPos pos) { schematic.removeTileEntity(pos); }
        @Override public boolean isEmpty() { return !containsAny(Integer.MIN_VALUE, Integer.MAX_VALUE); }
        @Override public boolean isEmptyBetween(int startY, int endY) { return !containsAny(startY, endY); }

        private boolean containsAny(int startY, int endY) {
            BlockPos anchor = schematic.getAnchor();
            for (BlockPos local : schematic.getAllPositions()) {
                BlockPos global = anchor.add(local);
                if (global.getX() >> 4 == chunkX && global.getZ() >> 4 == chunkZ
                    && global.getY() >= startY && global.getY() <= endY) return true;
            }
            return false;
        }
    }
}
