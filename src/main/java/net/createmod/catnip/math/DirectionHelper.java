package net.createmod.catnip.math;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;

public final class DirectionHelper {
    private DirectionHelper() {}
    public static EnumFacing rotateAround(EnumFacing direction, Axis axis) {
        switch (axis) {
            case X: return direction.getAxis() == Axis.X ? direction : rotateX(direction);
            case Y: return direction.getAxis() == Axis.Y ? direction : direction.rotateY();
            case Z: return direction.getAxis() == Axis.Z ? direction : rotateZ(direction);
            default: throw new IllegalStateException("Unknown axis " + axis);
        }
    }
    public static EnumFacing rotateX(EnumFacing direction) {
        switch (direction) {
            case NORTH: return EnumFacing.DOWN;
            case SOUTH: return EnumFacing.UP;
            case UP: return EnumFacing.NORTH;
            case DOWN: return EnumFacing.SOUTH;
            default: throw new IllegalStateException("Cannot rotate " + direction + " around X");
        }
    }
    public static EnumFacing rotateZ(EnumFacing direction) {
        switch (direction) {
            case EAST: return EnumFacing.DOWN;
            case WEST: return EnumFacing.UP;
            case UP: return EnumFacing.EAST;
            case DOWN: return EnumFacing.WEST;
            default: throw new IllegalStateException("Cannot rotate " + direction + " around Z");
        }
    }
    public static EnumFacing getPositivePerpendicular(Axis horizontalAxis) {
        if (horizontalAxis == Axis.Y) throw new IllegalArgumentException("Expected a horizontal axis");
        return horizontalAxis == Axis.X ? EnumFacing.SOUTH : EnumFacing.EAST;
    }
}
