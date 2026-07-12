package net.createmod.catnip.levelWrappers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/** Isolated in-memory schematic view. It never writes through to the wrapped world. */
public class SchematicLevel extends WrappedLevel implements SchematicLevelAccessor {
    private final Map<BlockPos, IBlockState> blocks = new LinkedHashMap<BlockPos, IBlockState>();
    private final Map<BlockPos, TileEntity> blockEntities = new LinkedHashMap<BlockPos, TileEntity>();
    private final List<Entity> entities = new ArrayList<Entity>();
    private AxisAlignedBB bounds = new AxisAlignedBB(BlockPos.ORIGIN);
    private BlockPos anchor;
    public boolean renderMode;

    public SchematicLevel(IBlockAccess original) { this(BlockPos.ORIGIN, original); }

    public SchematicLevel(BlockPos anchor, IBlockAccess original) {
        super(original);
        this.anchor = anchor == null ? BlockPos.ORIGIN : anchor.toImmutable();
    }

    public BlockPos getAnchor() { return anchor; }
    public void setAnchor(BlockPos anchor) {
        if (anchor == null) throw new IllegalArgumentException("anchor");
        this.anchor = anchor.toImmutable();
    }

    private BlockPos local(BlockPos global) { return global.subtract(anchor).toImmutable(); }

    @Override public Set<BlockPos> getAllPositions() {
        return Collections.unmodifiableSet(new LinkedHashSet<BlockPos>(blocks.keySet()));
    }
    @Override public List<Entity> getEntityList() { return Collections.unmodifiableList(entities); }
    @Override public Map<BlockPos, IBlockState> getBlockMap() { return Collections.unmodifiableMap(blocks); }
    @Override public AxisAlignedBB getBounds() { return bounds; }
    @Override public void setBounds(AxisAlignedBB bounds) {
        if (bounds == null) throw new IllegalArgumentException("bounds");
        this.bounds = bounds;
    }
    @Override public Iterable<TileEntity> getBlockEntities() {
        return Collections.unmodifiableCollection(blockEntities.values());
    }
    @Override public Iterable<TileEntity> getRenderedBlockEntities() { return getBlockEntities(); }

    public boolean addEntity(Entity entity) {
        if (entity == null || entities.contains(entity)) return false;
        return entities.add(entity);
    }

    @Override
    public IBlockState getBlockState(BlockPos globalPos) {
        IBlockState state = blocks.get(local(globalPos));
        return state == null ? Blocks.AIR.getDefaultState() : state;
    }

    @Nullable
    @Override
    public TileEntity getTileEntity(BlockPos globalPos) {
        return blockEntities.get(local(globalPos));
    }

    @Override
    public void setTileEntity(BlockPos globalPos, TileEntity tileEntity) {
        if (tileEntity == null) throw new IllegalArgumentException("tileEntity");
        BlockPos local = local(globalPos);
        tileEntity.setPos(globalPos.toImmutable());
        if (getWorld() != null) tileEntity.setWorld(getWorld());
        blockEntities.put(local, tileEntity);
        include(local);
    }

    public void removeTileEntity(BlockPos globalPos) {
        blockEntities.remove(local(globalPos));
    }

    @Override
    public boolean setBlockState(BlockPos globalPos, IBlockState state, int flags) {
        if (state == null) throw new IllegalArgumentException("state");
        BlockPos local = local(globalPos);
        IBlockState previous;
        if (state.getMaterial() == Material.AIR) {
            previous = blocks.remove(local);
            blockEntities.remove(local);
        } else {
            previous = blocks.put(local, state);
            include(local);
        }
        return previous == null || !previous.equals(state);
    }

    @Override public boolean isAirBlock(BlockPos pos) { return getBlockState(pos).getMaterial() == Material.AIR; }
    @Override public int getCombinedLight(BlockPos pos, int minimumBlockLight) {
        int block = Math.max(minimumBlockLight, getBlockState(pos).getLightValue(this, pos));
        return 15 << 20 | Math.min(15, block) << 4;
    }
    @Override public int getStrongPower(BlockPos pos, EnumFacing direction) {
        return getBlockState(pos).getStrongPower(this, pos, direction);
    }
    @Override public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean defaultValue) {
        return getBlockState(pos).isSideSolid(this, pos, side);
    }

    private void include(BlockPos local) {
        AxisAlignedBB blockBounds = new AxisAlignedBB(local, local.add(1, 1, 1));
        bounds = blocks.size() + blockEntities.size() == 1 ? blockBounds : bounds.union(blockBounds);
    }
}
