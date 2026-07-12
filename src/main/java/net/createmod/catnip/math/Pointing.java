package net.createmod.catnip.math;

import net.createmod.catnip.lang.Lang;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;

public enum Pointing implements IStringSerializable {
    UP(0), LEFT(270), DOWN(180), RIGHT(90);
    private final int xRotation;
    Pointing(int xRotation) { this.xRotation = xRotation; }
    public String getName() { return Lang.asId(name()); }
    public int getXRotation() { return xRotation; }
    public EnumFacing getCombinedDirection(EnumFacing direction) {
        EnumFacing.Axis axis = direction.getAxis();
        EnumFacing top = axis == EnumFacing.Axis.Y ? EnumFacing.SOUTH : EnumFacing.UP;
        int rotations = direction.getAxisDirection() == EnumFacing.AxisDirection.NEGATIVE ? (4 - ordinal()) % 4 : ordinal();
        for (int i = 0; i < rotations; i++) top = DirectionHelper.rotateAround(top, axis);
        return top;
    }
}
