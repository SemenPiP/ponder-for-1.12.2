package net.createmod.catnip.math;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

import net.createmod.catnip.data.Iterate;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

public class VoxelShaper {
    private final Map<EnumFacing, VoxelShape> shapes = new EnumMap<EnumFacing, VoxelShape>(EnumFacing.class);
    public VoxelShape get(EnumFacing direction) { return shapes.get(direction); }
    public VoxelShape get(Axis axis) { return get(axisAsFace(axis)); }
    public static VoxelShaper forHorizontal(VoxelShape shape, EnumFacing facing) {
        return forDirectionsWithRotation(shape, facing, Arrays.asList(Iterate.horizontalDirections), new HorizontalRotationValues());
    }
    public static VoxelShaper forHorizontalAxis(VoxelShape shape, Axis along) {
        return forDirectionsWithRotation(shape, axisAsFace(along), Arrays.asList(EnumFacing.SOUTH, EnumFacing.EAST), new HorizontalRotationValues());
    }
    public static VoxelShaper forDirectional(VoxelShape shape, EnumFacing facing) {
        return forDirectionsWithRotation(shape, facing, Arrays.asList(Iterate.directions), new DefaultRotationValues());
    }
    public static VoxelShaper forAxis(VoxelShape shape, Axis along) {
        return forDirectionsWithRotation(shape, axisAsFace(along), Arrays.asList(EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.UP), new DefaultRotationValues());
    }
    public VoxelShaper withVerticalShapes(VoxelShape upShape) {
        shapes.put(EnumFacing.UP, upShape); shapes.put(EnumFacing.DOWN, rotatedCopy(upShape, new Vec3d(180, 0, 0))); return this;
    }
    public VoxelShaper withShape(VoxelShape shape, EnumFacing facing) { shapes.put(facing, shape); return this; }
    public static EnumFacing axisAsFace(Axis axis) { return EnumFacing.getFacingFromAxis(EnumFacing.AxisDirection.POSITIVE, axis); }
    protected static float horizontalAngleFromDirection(EnumFacing direction) {
        return direction.getAxis() == Axis.Y ? 0 : direction.getHorizontalIndex() * 90f;
    }
    protected static VoxelShaper forDirectionsWithRotation(VoxelShape shape, EnumFacing from, Iterable<EnumFacing> directions,
                                                            Function<EnumFacing, Vec3d> values) {
        VoxelShaper shaper = new VoxelShaper();
        for (EnumFacing direction : directions) shaper.shapes.put(direction, rotate(shape, from, direction, values));
        return shaper;
    }
    protected static VoxelShape rotate(VoxelShape shape, EnumFacing from, EnumFacing to, Function<EnumFacing, Vec3d> values) {
        if (from == to) return shape;
        Vec3d rotation = values.apply(to).subtract(values.apply(from));
        return rotatedCopy(shape, rotation);
    }
    public static VoxelShape rotatedCopy(VoxelShape shape, Vec3d rotation) {
        if (rotation.lengthSquared() == 0) return shape;
        java.util.List<VoxelShape> result = new java.util.ArrayList<VoxelShape>();
        for (AxisAlignedBB box : shape.getBoundingBoxes()) {
            Vec3d[] corners = new Vec3d[8];
            int index = 0;
            for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) {
                Vec3d corner = new Vec3d(x == 0 ? box.minX : box.maxX, y == 0 ? box.minY : box.maxY, z == 0 ? box.minZ : box.maxZ)
                    .subtract(new Vec3d(.5, .5, .5));
                corners[index++] = VecHelper.rotate(corner, rotation).add(new Vec3d(.5, .5, .5));
            }
            double minX = Double.POSITIVE_INFINITY, minY = minX, minZ = minX;
            double maxX = Double.NEGATIVE_INFINITY, maxY = maxX, maxZ = maxX;
            for (Vec3d corner : corners) {
                minX = Math.min(minX, corner.x); minY = Math.min(minY, corner.y); minZ = Math.min(minZ, corner.z);
                maxX = Math.max(maxX, corner.x); maxY = Math.max(maxY, corner.y); maxZ = Math.max(maxZ, corner.z);
            }
            result.add(VoxelShape.cuboid(minX, minY, minZ, maxX, maxY, maxZ));
        }
        return VoxelShape.union(result.toArray(new VoxelShape[result.size()]));
    }
    protected static class DefaultRotationValues implements Function<EnumFacing, Vec3d> {
        public Vec3d apply(EnumFacing direction) {
            return new Vec3d(direction == EnumFacing.UP ? 0 : direction.getAxis() == Axis.Y ? 180 : 90,
                -horizontalAngleFromDirection(direction), 0);
        }
    }
    protected static class HorizontalRotationValues implements Function<EnumFacing, Vec3d> {
        public Vec3d apply(EnumFacing direction) { return new Vec3d(0, -horizontalAngleFromDirection(direction), 0); }
    }
}
