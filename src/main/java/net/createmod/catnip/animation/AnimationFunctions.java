package net.createmod.catnip.animation;

public final class AnimationFunctions {
    private AnimationFunctions() {}

    public static float easeOut(float t) {
        return (float) Math.sin(Math.PI * .5D * clamp01(t));
    }

    public static float easeInOut(float t) {
        float sine = (float) Math.sin(Math.PI * .5D * clamp01(t));
        return sine * sine;
    }

    public static float easeIn(float t) {
        return (float) Math.pow(clamp01(t), 1.7D);
    }

    private static float clamp01(float value) {
        return Math.max(0, Math.min(1, value));
    }
}
