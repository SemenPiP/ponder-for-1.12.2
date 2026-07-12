package net.createmod.catnip.math;

import net.minecraft.util.EnumFacing;

public final class AngleHelper {
    private AngleHelper() {}
    public static float horizontalAngle(EnumFacing facing) {
        if (facing.getAxis() == EnumFacing.Axis.Y) return 0;
        float angle = facing.getHorizontalAngle();
        return facing.getAxis() == EnumFacing.Axis.X ? -angle : angle;
    }
    public static float verticalAngle(EnumFacing facing) {
        return facing == EnumFacing.UP ? -90 : facing == EnumFacing.DOWN ? 90 : 0;
    }
    public static float rad(double angle) { return angle == 0 ? 0 : (float) Math.toRadians(angle); }
    public static float deg(double angle) { return angle == 0 ? 0 : (float) Math.toDegrees(angle); }
    public static float angleLerp(double progress, double current, double target) {
        return (float) (current + getShortestAngleDiff(current, target) * progress);
    }
    public static float getShortestAngleDiff(double current, double target) {
        return (float) ((((target - current) % 360 + 540) % 360) - 180);
    }
    public static float getShortestAngleDiff(double current, double target, float hint) {
        float diff = getShortestAngleDiff(current, target);
        if (Math.abs(Math.abs(diff) - 180) < 1.0E-5F && Math.signum(diff) != Math.signum(hint))
            diff += 360 * Math.signum(hint);
        return diff;
    }
    public static float wrapAngle180(float angle) {
        float wrapped = angle % 360;
        if (wrapped >= 180) wrapped -= 360;
        if (wrapped < -180) wrapped += 360;
        return wrapped;
    }
}
