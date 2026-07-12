package net.createmod.catnip.levelWrappers;

import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;

/**
 * 1.12 adaptation of Catnip's level wrapper. Read operations are true delegates;
 * mutations are available when the wrapped access is a real {@link World}.
 */
public class WrappedLevel implements IBlockAccess {
    protected final IBlockAccess level;

    public WrappedLevel(IBlockAccess level) {
        if (level == null) throw new IllegalArgumentException("level");
        this.level = level;
    }

    public IBlockAccess getLevel() { return level; }
    @Nullable public World getWorld() { return level instanceof World ? (World) level : null; }

    public boolean setBlockState(BlockPos pos, IBlockState state, int flags) {
        World world = requireWorld("setBlockState");
        return world.setBlockState(pos, state, flags);
    }

    public boolean spawnEntity(Entity entity) {
        return requireWorld("spawnEntity").spawnEntity(entity);
    }

    protected World requireWorld(String operation) {
        World world = getWorld();
        if (world == null) throw new UnsupportedOperationException(operation + " requires a World-backed wrapper");
        return world;
    }

    @Override public TileEntity getTileEntity(BlockPos pos) { return level.getTileEntity(pos); }
    @Override public int getCombinedLight(BlockPos pos, int lightValue) { return level.getCombinedLight(pos, lightValue); }
    @Override public IBlockState getBlockState(BlockPos pos) { return level.getBlockState(pos); }
    @Override public boolean isAirBlock(BlockPos pos) { return level.isAirBlock(pos); }
    @Override public Biome getBiome(BlockPos pos) { return level.getBiome(pos); }
    @Override public int getStrongPower(BlockPos pos, EnumFacing direction) { return level.getStrongPower(pos, direction); }
    @Override public WorldType getWorldType() { return level.getWorldType(); }
    @Override public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean defaultValue) {
        return level.isSideSolid(pos, side, defaultValue);
    }
}
