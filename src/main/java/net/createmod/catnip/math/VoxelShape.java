package net.createmod.catnip.math;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

public final class VoxelShape {
    public static final VoxelShape EMPTY = new VoxelShape(Collections.<AxisAlignedBB>emptyList());
    public static final VoxelShape FULL_CUBE = cuboid(0, 0, 0, 1, 1, 1);
    private final List<AxisAlignedBB> boxes;

    private VoxelShape(List<AxisAlignedBB> boxes) {
        this.boxes = Collections.unmodifiableList(new ArrayList<AxisAlignedBB>(boxes));
    }

    public static VoxelShape cuboid(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (!finite(minX, minY, minZ, maxX, maxY, maxZ)) throw new IllegalArgumentException("Box coordinates must be finite");
        double x1 = Math.min(minX, maxX), y1 = Math.min(minY, maxY), z1 = Math.min(minZ, maxZ);
        double x2 = Math.max(minX, maxX), y2 = Math.max(minY, maxY), z2 = Math.max(minZ, maxZ);
        if (x1 == x2 || y1 == y2 || z1 == z2) return EMPTY;
        return new VoxelShape(Collections.singletonList(new AxisAlignedBB(x1, y1, z1, x2, y2, z2)));
    }

    public static VoxelShape box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return cuboid(minX / 16D, minY / 16D, minZ / 16D, maxX / 16D, maxY / 16D, maxZ / 16D);
    }

    public static VoxelShape union(VoxelShape... shapes) {
        List<AxisAlignedBB> result = new ArrayList<AxisAlignedBB>();
        for (VoxelShape shape : shapes) if (shape != null) result.addAll(shape.boxes);
        return result.isEmpty() ? EMPTY : new VoxelShape(result);
    }

    public boolean isEmpty() { return boxes.isEmpty(); }
    public List<AxisAlignedBB> getBoundingBoxes() { return boxes; }
    public AxisAlignedBB getBounds() {
        if (boxes.isEmpty()) throw new IllegalStateException("Empty shape has no bounds");
        AxisAlignedBB result = boxes.get(0);
        for (int i = 1; i < boxes.size(); i++) result = result.union(boxes.get(i));
        return result;
    }
    public VoxelShape offset(double x, double y, double z) {
        List<AxisAlignedBB> moved = new ArrayList<AxisAlignedBB>(boxes.size());
        for (AxisAlignedBB box : boxes) moved.add(box.offset(x, y, z));
        return moved.isEmpty() ? EMPTY : new VoxelShape(moved);
    }
    public void forAllBoxes(Consumer<AxisAlignedBB> consumer) { for (AxisAlignedBB box : boxes) consumer.accept(box); }
    public boolean contains(Vec3d point) {
        for (AxisAlignedBB box : boxes)
            if (point.x >= box.minX && point.x <= box.maxX && point.y >= box.minY && point.y <= box.maxY && point.z >= box.minZ && point.z <= box.maxZ)
                return true;
        return false;
    }
    @Nullable public RayTraceResult rayTrace(Vec3d start, Vec3d end) {
        RayTraceResult closest = null;
        double distance = Double.POSITIVE_INFINITY;
        for (AxisAlignedBB box : boxes) {
            RayTraceResult hit = box.calculateIntercept(start, end);
            if (hit != null) {
                double candidate = hit.hitVec.squareDistanceTo(start);
                if (candidate < distance) { distance = candidate; closest = hit; }
            }
        }
        return closest;
    }
    private static boolean finite(double... values) {
        for (double value : values) if (Double.isNaN(value) || Double.isInfinite(value)) return false;
        return true;
    }
}
