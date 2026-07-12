package net.createmod.ponder.foundation;

import net.createmod.ponder.api.scene.PositionUtil;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.api.scene.SelectionUtil;
import net.createmod.ponder.api.scene.VectorUtil;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public final class PonderSceneBuildingUtil implements SceneBuildingUtil {
    private final BlockPos min;
    private final BlockPos max;
    private final SelectionUtil selection = new SelectionHelper();
    private final VectorUtil vectors = new VectorHelper();
    private final PositionUtil positions = new PositionHelper();

    public PonderSceneBuildingUtil(BlockPos min, BlockPos max) {
        this.min = new BlockPos(Math.min(min.getX(), max.getX()), Math.min(min.getY(), max.getY()), Math.min(min.getZ(), max.getZ()));
        this.max = new BlockPos(Math.max(min.getX(), max.getX()), Math.max(min.getY(), max.getY()), Math.max(min.getZ(), max.getZ()));
    }

    public PonderSceneBuildingUtil(BlockPos size) {
        this(BlockPos.ORIGIN, new BlockPos(Math.max(0, size.getX() - 1), Math.max(0, size.getY() - 1), Math.max(0, size.getZ() - 1)));
    }

    @Override
    public SelectionUtil select() {
        return selection;
    }

    @Override
    public VectorUtil vector() {
        return vectors;
    }

    @Override
    public PositionUtil grid() {
        return positions;
    }

    private final class PositionHelper implements PositionUtil {
        @Override
        public BlockPos at(int x, int y, int z) {
            return new BlockPos(x, y, z);
        }

        @Override
        public BlockPos zero() {
            return BlockPos.ORIGIN;
        }
    }

    private final class VectorHelper implements VectorUtil {
        @Override
        public Vec3d centerOf(int x, int y, int z) {
            return centerOf(new BlockPos(x, y, z));
        }

        @Override
        public Vec3d centerOf(BlockPos pos) {
            return new Vec3d(pos).add(.5, .5, .5);
        }

        @Override
        public Vec3d topOf(int x, int y, int z) {
            return topOf(new BlockPos(x, y, z));
        }

        @Override
        public Vec3d topOf(BlockPos pos) {
            return blockSurface(pos, EnumFacing.UP);
        }

        @Override
        public Vec3d blockSurface(BlockPos pos, EnumFacing face) {
            return blockSurface(pos, face, 0);
        }

        @Override
        public Vec3d blockSurface(BlockPos pos, EnumFacing face, float margin) {
            Vec3d normal = new Vec3d(face.getDirectionVec());
            return centerOf(pos).add(normal.scale(.5 + margin));
        }

        @Override
        public Vec3d of(double x, double y, double z) {
            return new Vec3d(x, y, z);
        }
    }

    private final class SelectionHelper implements SelectionUtil {
        @Override
        public Selection everywhere() {
            return SelectionImpl.of(min, max);
        }

        @Override
        public Selection position(int x, int y, int z) {
            return position(new BlockPos(x, y, z));
        }

        @Override
        public Selection position(BlockPos pos) {
            return SelectionImpl.of(pos);
        }

        @Override
        public Selection fromTo(int x, int y, int z, int x2, int y2, int z2) {
            return fromTo(new BlockPos(x, y, z), new BlockPos(x2, y2, z2));
        }

        @Override
        public Selection fromTo(BlockPos pos1, BlockPos pos2) {
            return SelectionImpl.of(pos1, pos2);
        }

        @Override
        public Selection column(int x, int z) {
            return fromTo(x, min.getY(), z, x, max.getY(), z);
        }

        @Override
        public Selection layer(int y) {
            return layers(y, 1);
        }

        @Override
        public Selection layersFrom(int y) {
            return fromTo(min.getX(), Math.max(y, min.getY()), min.getZ(), max.getX(), max.getY(), max.getZ());
        }

        @Override
        public Selection layers(int y, int height) {
            if (height <= 0)
                return SelectionImpl.empty();
            int start = Math.max(y, min.getY());
            int end = Math.min(max.getY(), y + height - 1);
            if (end < start)
                return SelectionImpl.empty();
            return fromTo(min.getX(), start, min.getZ(), max.getX(), end, max.getZ());
        }

        @Override
        public Selection cuboid(BlockPos origin, Vec3i size) {
            return SelectionImpl.of(origin, origin.add(size));
        }
    }
}
