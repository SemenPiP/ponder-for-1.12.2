package net.createmod.ponder.api.scene;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public interface VectorUtil {
    Vec3d centerOf(int x, int y, int z);

    Vec3d centerOf(BlockPos pos);

    Vec3d topOf(int x, int y, int z);

    Vec3d topOf(BlockPos pos);

    Vec3d blockSurface(BlockPos pos, EnumFacing face);

    Vec3d blockSurface(BlockPos pos, EnumFacing face, float margin);

    Vec3d of(double x, double y, double z);
}
