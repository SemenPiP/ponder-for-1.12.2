package net.createmod.catnip.animation;

import javax.annotation.Nullable;

import net.createmod.catnip.math.AngleHelper;
import net.minecraft.nbt.NBTTagCompound;

public class LerpedFloat {
    protected Interpolator interpolator;
    protected float previousValue;
    protected float value;
    @Nullable protected Chaser chaseFunction;
    protected float chaseTarget;
    protected float chaseSpeed;
    protected boolean angularChase;
    protected boolean forcedSync;

    public LerpedFloat(Interpolator interpolator) {
        if (interpolator == null) throw new NullPointerException("interpolator");
        this.interpolator = interpolator;
        startWithValue(0);
        forcedSync = true;
    }

    public static LerpedFloat linear() {
        return new LerpedFloat(new Interpolator() {
            public float interpolate(double progress, double current, double target) {
                return (float) (current + (target - current) * progress);
            }
        });
    }

    public static LerpedFloat angular() {
        LerpedFloat value = new LerpedFloat(new Interpolator() {
            public float interpolate(double progress, double current, double target) {
                return AngleHelper.angleLerp(progress, current, target);
            }
        });
        value.angularChase = true;
        return value;
    }

    public LerpedFloat startWithValue(double value) {
        float f = (float) value;
        this.previousValue = f;
        this.chaseTarget = f;
        this.value = f;
        return this;
    }

    public LerpedFloat chase(double target, double speed, Chaser chaser) {
        if (chaser == null) throw new NullPointerException("chaser");
        updateChaseTarget((float) target);
        chaseSpeed = Math.max(0, (float) speed);
        chaseFunction = chaser;
        return this;
    }

    public LerpedFloat chaseTimed(double target, int ticks) {
        if (ticks <= 0) return startWithValue(target);
        return chase(target, Math.abs(target - value) / ticks, Chaser.LINEAR);
    }

    public LerpedFloat disableSmartAngleChasing() { angularChase = false; return this; }

    public void updateChaseTarget(float target) {
        chaseTarget = angularChase ? value + AngleHelper.getShortestAngleDiff(value, target) : target;
    }

    public boolean updateChaseSpeed(double speed) {
        float old = chaseSpeed;
        chaseSpeed = Math.max(0, (float) speed);
        return !equal(old, chaseSpeed);
    }

    public void tickChaser() {
        previousValue = value;
        if (chaseFunction == null) return;
        if (equal(value, chaseTarget)) value = chaseTarget;
        else value = chaseFunction.chase(value, chaseSpeed, chaseTarget);
    }

    public void setValueNoUpdate(double value) { this.value = (float) value; }
    public void setValue(double value) { previousValue = this.value; this.value = (float) value; }
    public float getValue() { return getValue(1); }
    public float getValue(float partialTicks) { return interpolator.interpolate(partialTicks, previousValue, value); }
    public boolean settled() {
        return equal(previousValue, value) && (chaseFunction == null || equal(value, chaseTarget));
    }
    public float getChaseTarget() { return chaseTarget; }
    public void forceNextSync() { forcedSync = true; }

    public NBTTagCompound writeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setFloat("Speed", chaseSpeed);
        tag.setFloat("Target", chaseTarget);
        tag.setFloat("Value", value);
        if (forcedSync) tag.setBoolean("Force", true);
        forcedSync = false;
        return tag;
    }

    public void readNBT(NBTTagCompound tag, boolean clientPacket) {
        if (!clientPacket || tag.getBoolean("Force")) startWithValue(tag.getFloat("Value"));
        chaseSpeed = Math.max(0, tag.getFloat("Speed"));
        chaseTarget = tag.getFloat("Target");
    }

    private static boolean equal(double a, double b) { return Math.abs(a - b) < 1.0E-5D; }

    public interface Interpolator { float interpolate(double progress, double current, double target); }

    public interface Chaser {
        Chaser IDLE = new Chaser() { public float chase(double c, double s, double t) { return (float) c; } };
        Chaser EXP = exp(Double.MAX_VALUE);
        Chaser LINEAR = new Chaser() {
            public float chase(double c, double s, double t) {
                return (float) (c + clamp(t - c, -s, s));
            }
        };

        float chase(double current, double speed, double target);

        static Chaser exp(final double maxEffectiveSpeed) {
            return new Chaser() {
                public float chase(double c, double s, double t) {
                    return (float) (c + clamp((t - c) * s, -maxEffectiveSpeed, maxEffectiveSpeed));
                }
            };
        }

        static double clamp(double value, double min, double max) {
            return Math.max(min, Math.min(max, value));
        }
    }
}
