package net.createmod.catnip.levelWrappers;

import java.util.function.BiFunction;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/** IBlockAccess view that substitutes states for collision/ray tracing without mutating the source. */
public class RayTraceLevel extends WrappedLevel {
    private final BiFunction<BlockPos, IBlockState, IBlockState> stateGetter;

    public RayTraceLevel(IBlockAccess template,
                         BiFunction<BlockPos, IBlockState, IBlockState> stateGetter) {
        super(template);
        if (stateGetter == null) throw new IllegalArgumentException("stateGetter");
        this.stateGetter = stateGetter;
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        IBlockState state = stateGetter.apply(pos, level.getBlockState(pos));
        if (state == null) throw new IllegalStateException("RayTraceLevel state function returned null at " + pos);
        return state;
    }

    @Override public boolean isAirBlock(BlockPos pos) { return getBlockState(pos).getMaterial() == Material.AIR; }
    @Override public int getStrongPower(BlockPos pos, EnumFacing direction) {
        return getBlockState(pos).getStrongPower(this, pos, direction);
    }
    @Override public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean defaultValue) {
        return getBlockState(pos).isSideSolid(this, pos, side);
    }
}
