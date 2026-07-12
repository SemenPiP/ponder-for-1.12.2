package net.createmod.catnip.math;

import java.io.IOException;
import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public final class VecHelper {
    public static final Vec3d CENTER_OF_ORIGIN = new Vec3d(.5, .5, .5);
    private VecHelper() {}
    public static Vec3d rotate(Vec3d vector, Vec3d rotation) { return rotate(vector, rotation.x, rotation.y, rotation.z); }
    public static Vec3d rotate(Vec3d vector, double x, double y, double z) {
        return rotate(rotate(rotate(vector, x, EnumFacing.Axis.X), y, EnumFacing.Axis.Y), z, EnumFacing.Axis.Z);
    }
    public static Vec3d rotateCentered(Vec3d vector, double degrees, EnumFacing.Axis axis) {
        return rotate(vector.subtract(CENTER_OF_ORIGIN), degrees, axis).add(CENTER_OF_ORIGIN);
    }
    public static Vec3d rotate(Vec3d vector, double degrees, EnumFacing.Axis axis) {
        if (degrees == 0 || vector.lengthSquared() == 0) return vector;
        double angle = Math.toRadians(degrees), sin = Math.sin(angle), cos = Math.cos(angle);
        double x = vector.x, y = vector.y, z = vector.z;
        switch (axis) {
            case X: return new Vec3d(x, y * cos - z * sin, z * cos + y * sin);
            case Y: return new Vec3d(x * cos + z * sin, y, z * cos - x * sin);
            case Z: return new Vec3d(x * cos - y * sin, y * cos + x * sin, z);
            default: return vector;
        }
    }
    public static Vec3d mirrorCentered(Vec3d vector, Mirror mirror) {
        return mirror(vector.subtract(CENTER_OF_ORIGIN), mirror).add(CENTER_OF_ORIGIN);
    }
    public static Vec3d mirror(Vec3d vector, Mirror mirror) {
        if (mirror == Mirror.LEFT_RIGHT) return new Vec3d(vector.x, vector.y, -vector.z);
        if (mirror == Mirror.FRONT_BACK) return new Vec3d(-vector.x, vector.y, vector.z);
        return vector;
    }
    public static Vec3d lookAt(Vec3d vector, Vec3d forward) {
        if (forward.lengthSquared() == 0) return vector;
        Vec3d fwd = forward.normalize();
        Vec3d up = new Vec3d(0, 1, 0);
        double dot = fwd.dotProduct(up);
        if (Math.abs(dot) > .999) up = new Vec3d(0, 0, dot > 0 ? 1 : -1);
        Vec3d right = fwd.crossProduct(up).normalize();
        up = right.crossProduct(fwd).normalize();
        return new Vec3d(vector.x * right.x + vector.y * up.x + vector.z * fwd.x,
            vector.x * right.y + vector.y * up.y + vector.z * fwd.y,
            vector.x * right.z + vector.y * up.z + vector.z * fwd.z);
    }
    public static boolean isVecPointingTowards(Vec3d vector, EnumFacing direction) {
        return vector.lengthSquared() > 0 && new Vec3d(direction.getDirectionVec()).dotProduct(vector.normalize()) > .125;
    }
    public static Vec3d getCenterOf(Vec3i pos) { return new Vec3d(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5); }
    public static Vec3d offsetRandomly(Vec3d vector, Random random, float radius) {
        return vector.add((random.nextFloat() - .5) * 2 * radius, (random.nextFloat() - .5) * 2 * radius,
            (random.nextFloat() - .5) * 2 * radius);
    }
    public static Vec3d axisAlingedPlaneOf(Vec3d vector) {
        if (vector.lengthSquared() == 0) return new Vec3d(1, 1, 1);
        Vec3d normal = vector.normalize(); return new Vec3d(1 - Math.abs(normal.x), 1 - Math.abs(normal.y), 1 - Math.abs(normal.z));
    }
    public static Vec3d axisAlingedPlaneOf(EnumFacing face) { return axisAlingedPlaneOf(new Vec3d(face.getDirectionVec())); }
    public static NBTTagList writeNBT(Vec3d vector) {
        NBTTagList list = new NBTTagList(); list.appendTag(new NBTTagDouble(vector.x)); list.appendTag(new NBTTagDouble(vector.y)); list.appendTag(new NBTTagDouble(vector.z)); return list;
    }
    public static NBTTagCompound writeNBTCompound(Vec3d vector) { NBTTagCompound tag = new NBTTagCompound(); tag.setTag("V", writeNBT(vector)); return tag; }
    public static Vec3d readNBT(NBTTagList list) {
        return list.tagCount() < 3 ? Vec3d.ZERO : new Vec3d(list.getDoubleAt(0), list.getDoubleAt(1), list.getDoubleAt(2));
    }
    public static Vec3d readNBTCompound(NBTTagCompound tag) { return readNBT(tag.getTagList("V", 6)); }
    public static void write(Vec3d vector, PacketBuffer buffer) { buffer.writeDouble(vector.x); buffer.writeDouble(vector.y); buffer.writeDouble(vector.z); }
    public static Vec3d read(PacketBuffer buffer) { return new Vec3d(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()); }
    public static Vec3d voxelSpace(double x, double y, double z) { return new Vec3d(x / 16D, y / 16D, z / 16D); }
    public static int getCoordinate(Vec3i pos, EnumFacing.Axis axis) {
        switch (axis) { case X: return pos.getX(); case Y: return pos.getY(); default: return pos.getZ(); }
    }
    public static float getCoordinate(Vec3d pos, EnumFacing.Axis axis) {
        return (float) (axis == EnumFacing.Axis.X ? pos.x : axis == EnumFacing.Axis.Y ? pos.y : pos.z);
    }
    public static boolean onSameAxis(BlockPos first, BlockPos second, EnumFacing.Axis axis) {
        for (EnumFacing.Axis other : EnumFacing.Axis.values()) if (other != axis && getCoordinate(first, other) != getCoordinate(second, other)) return false;
        return true;
    }
    public static Vec3d clamp(Vec3d vector, float maxLength) {
        if (maxLength < 0) throw new IllegalArgumentException("maxLength cannot be negative");
        return vector.lengthSquared() > maxLength * maxLength ? vector.normalize().scale(maxLength) : vector;
    }
    public static Vec3d lerp(float progress, Vec3d from, Vec3d to) { return from.add(to.subtract(from).scale(progress)); }
    public static Vec3d slerp(float progress, Vec3d from, Vec3d to) {
        double fromLength = from.length(), toLength = to.length();
        if (fromLength < 1.0E-9 || toLength < 1.0E-9) return lerp(progress, from, to);
        Vec3d a = from.scale(1 / fromLength), b = to.scale(1 / toLength);
        double dot = Math.max(-1, Math.min(1, a.dotProduct(b)));
        double theta = Math.acos(dot);
        if (theta < 1.0E-5 || Math.abs(Math.PI - theta) < 1.0E-5) return lerp(progress, from, to);
        double sin = Math.sin(theta);
        Vec3d direction = a.scale(Math.sin((1 - progress) * theta) / sin).add(b.scale(Math.sin(progress * theta) / sin));
        return direction.scale(fromLength + (toLength - fromLength) * progress);
    }
    public static Vec3d clampComponentWise(Vec3d vector, float max) {
        return new Vec3d(clamp(vector.x, -max, max), clamp(vector.y, -max, max), clamp(vector.z, -max, max));
    }
    public static Vec3d componentMin(Vec3d a, Vec3d b) { return new Vec3d(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z)); }
    public static Vec3d componentMax(Vec3d a, Vec3d b) { return new Vec3d(Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z)); }
    public static Vec3d project(Vec3d vector, Vec3d onto) { return onto.lengthSquared() == 0 ? Vec3d.ZERO : onto.scale(vector.dotProduct(onto) / onto.lengthSquared()); }
    @Nullable public static Vec3d intersectSphere(Vec3d origin, Vec3d direction, Vec3d center, double radius) {
        if (radius < 0 || direction.lengthSquared() == 0) return null;
        Vec3d unit = direction.normalize(), diff = origin.subtract(center);
        double projection = unit.dotProduct(diff), discriminant = projection * projection - (diff.lengthSquared() - radius * radius);
        if (discriminant < 0) return null;
        double root = Math.sqrt(discriminant), near = -projection - root, far = -projection + root;
        double distance = near >= 0 ? near : far >= 0 ? far : Double.NaN;
        return Double.isNaN(distance) ? null : origin.add(unit.scale(distance));
    }
    public static Vec3d bezier(Vec3d p1, Vec3d p2, Vec3d q1, Vec3d q2, float t) {
        Vec3d v1 = lerp(t, p1, q1), v2 = lerp(t, q1, q2), v3 = lerp(t, q2, p2);
        return lerp(t, lerp(t, v1, v2), lerp(t, v2, v3));
    }
    public static Vec3d bezierDerivative(Vec3d p1, Vec3d p2, Vec3d q1, Vec3d q2, float t) {
        return p1.scale(-3 * t * t + 6 * t - 3).add(q1.scale(9 * t * t - 12 * t + 3))
            .add(q2.scale(-9 * t * t + 6 * t)).add(p2.scale(3 * t * t));
    }
    @Nullable public static double[] intersectRanged(Vec3d p1, Vec3d q1, Vec3d p2, Vec3d q2, EnumFacing.Axis plane) {
        Vec3d first = p2.subtract(p1), second = q2.subtract(q1);
        if (first.lengthSquared() == 0 || second.lengthSquared() == 0) return null;
        double[] hit = intersect(p1, q1, first.normalize(), second.normalize(), plane);
        if (hit == null || hit[0] < 0 || hit[1] < 0 || hit[0] * hit[0] > first.lengthSquared() || hit[1] * hit[1] > second.lengthSquared()) return null;
        return hit;
    }
    @Nullable public static double[] intersect(Vec3d p1, Vec3d p2, Vec3d r, Vec3d s, EnumFacing.Axis plane) {
        double p1a, p1b, p2a, p2b, ra, rb, sa, sb;
        if (plane == EnumFacing.Axis.X) { p1a=p1.y;p1b=p1.z;p2a=p2.y;p2b=p2.z;ra=r.y;rb=r.z;sa=s.y;sb=s.z; }
        else if (plane == EnumFacing.Axis.Y) { p1a=p1.x;p1b=p1.z;p2a=p2.x;p2b=p2.z;ra=r.x;rb=r.z;sa=s.x;sb=s.z; }
        else { p1a=p1.x;p1b=p1.y;p2a=p2.x;p2b=p2.y;ra=r.x;rb=r.y;sa=s.x;sb=s.y; }
        double cross = ra * sb - rb * sa;
        if (Math.abs(cross) < 1.0E-9) return null;
        double qa = p2a - p1a, qb = p2b - p1b;
        return new double[]{(qa * sb - qb * sa) / cross, (qa * rb - qb * ra) / cross};
    }
    public static double alignedDistanceToFace(Vec3d pos, BlockPos block, EnumFacing face) {
        double coordinate = getCoordinate(pos, face.getAxis());
        int blockCoordinate = getCoordinate(block, face.getAxis());
        return Math.abs(coordinate - (blockCoordinate + (face.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE ? 1 : 0)));
    }
    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
}
