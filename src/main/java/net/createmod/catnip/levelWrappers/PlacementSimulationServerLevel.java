package net.createmod.catnip.levelWrappers;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

/** Transactional block overlay for testing placement against a server world. */
public class PlacementSimulationServerLevel extends WrappedServerLevel {
    public final Map<BlockPos, IBlockState> blocksAdded = new LinkedHashMap<BlockPos, IBlockState>();

    public PlacementSimulationServerLevel(WorldServer wrapped) {
        super(wrapped);
    }

    public void clear() { blocksAdded.clear(); }
    public Map<BlockPos, IBlockState> getChanges() {
        return Collections.unmodifiableMap(new LinkedHashMap<BlockPos, IBlockState>(blocksAdded));
    }

    @Override
    public boolean setBlockState(BlockPos pos, IBlockState state, int flags) {
        if (pos == null || state == null) throw new IllegalArgumentException("pos/state");
        IBlockState previous = getBlockState(pos);
        blocksAdded.put(pos.toImmutable(), state);
        return !previous.equals(state);
    }

    public boolean setBlockAndUpdate(BlockPos pos, IBlockState state) {
        return setBlockState(pos, state, 3);
    }

    public boolean isStateAtPosition(BlockPos pos, Predicate<IBlockState> condition) {
        if (condition == null) throw new IllegalArgumentException("condition");
        return condition.test(getBlockState(pos));
    }

    public boolean isLoaded(BlockPos pos) {
        return blocksAdded.containsKey(pos) || serverLevel.isBlockLoaded(pos);
    }

    public boolean isAreaLoaded(BlockPos center, int range) {
        return serverLevel.isAreaLoaded(center, range);
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        IBlockState changed = blocksAdded.get(pos);
        return changed == null ? serverLevel.getBlockState(pos) : changed;
    }

    @Nullable
    @Override
    public TileEntity getTileEntity(BlockPos pos) {
        if (blocksAdded.containsKey(pos)) return null;
        return serverLevel.getTileEntity(pos);
    }

    @Override public boolean isAirBlock(BlockPos pos) { return getBlockState(pos).getMaterial() == Material.AIR; }
    @Override public int getStrongPower(BlockPos pos, EnumFacing direction) {
        return getBlockState(pos).getStrongPower(this, pos, direction);
    }
    @Override public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean defaultValue) {
        return getBlockState(pos).isSideSolid(this, pos, side);
    }

    /** Applies the captured transaction to the wrapped world only when explicitly requested. */
    public int commit(int flags) {
        int changed = 0;
        for (Map.Entry<BlockPos, IBlockState> entry : new LinkedHashMap<BlockPos, IBlockState>(blocksAdded).entrySet())
            if (serverLevel.setBlockState(entry.getKey(), entry.getValue(), flags)) changed++;
        blocksAdded.clear();
        return changed;
    }
}
