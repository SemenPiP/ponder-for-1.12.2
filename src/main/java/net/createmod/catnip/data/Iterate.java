package net.createmod.catnip.data;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.BlockPos;

public final class Iterate {
    public static final boolean[] trueAndFalse = {true, false};
    public static final boolean[] falseAndTrue = {false, true};
    public static final int[] zeroAndOne = {0, 1};
    public static final int[] positiveAndNegative = {1, -1};
    public static final EnumFacing[] directions = EnumFacing.values();
    public static final EnumFacing[] horizontalDirections = {
        EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.NORTH, EnumFacing.EAST
    };
    public static final Axis[] axes = Axis.values();
    public static final EnumSet<Axis> axisSet = EnumSet.allOf(Axis.class);
    private Iterate() {}
    public static EnumFacing[] directionsInAxis(Axis axis) {
        switch (axis) {
            case X: return new EnumFacing[]{EnumFacing.EAST, EnumFacing.WEST};
            case Y: return new EnumFacing[]{EnumFacing.UP, EnumFacing.DOWN};
            default: return new EnumFacing[]{EnumFacing.SOUTH, EnumFacing.NORTH};
        }
    }
    public static List<BlockPos> hereAndBelow(BlockPos pos) { return Arrays.asList(pos, pos.down()); }
    public static List<BlockPos> hereBelowAndAbove(BlockPos pos) { return Arrays.asList(pos, pos.down(), pos.up()); }
    public static <T> T cycleValue(List<T> list, T current) {
        if (list.isEmpty()) throw new IllegalArgumentException("Cannot cycle an empty list");
        int currentIndex = list.indexOf(current);
        if (currentIndex < 0) throw new IllegalArgumentException("Current value not found in list");
        return list.get((currentIndex + 1) % list.size());
    }
}
