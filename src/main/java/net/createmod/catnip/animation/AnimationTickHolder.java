package net.createmod.catnip.animation;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.world.World;

public final class AnimationTickHolder {
    private static final int WRAP_TICKS = 1728000;
    private static int ticks;
    private static int pausedTicks;
    private static volatile PartialTickSource partialTickSource = new PartialTickSource() {
        public float get(boolean pausedAware) { return 0; }
    };
    private static final Map<World, TimeSource> worldSources = new WeakHashMap<World, TimeSource>();

    private AnimationTickHolder() {}
    public static void reset() { ticks = 0; pausedTicks = 0; }
    public static void tick() { tick(false); }
    public static void tick(boolean paused) {
        if (paused) pausedTicks = (pausedTicks + 1) % WRAP_TICKS;
        else ticks = (ticks + 1) % WRAP_TICKS;
    }
    public static int getTicks() { return getTicks(false); }
    public static int getTicks(boolean includePaused) { return includePaused ? ticks + pausedTicks : ticks; }
    public static synchronized int getTicks(World world) {
        TimeSource source = worldSources.get(world);
        return source == null ? getTicks() : source.getTicks();
    }
    public static synchronized float getPartialTicks(World world) {
        TimeSource source = worldSources.get(world);
        return source == null ? getPartialTicks() : source.getPartialTicks();
    }
    public static float getRenderTime() { return getTicks() + getPartialTicks(); }
    public static float getRenderTime(World world) { return getTicks(world) + getPartialTicks(world); }
    public static float getPartialTicks() { return clampPartial(partialTickSource.get(true)); }
    public static float getPartialTicksUI() { return clampPartial(partialTickSource.get(false)); }
    public static void setPartialTickSource(PartialTickSource source) {
        partialTickSource = source == null ? new PartialTickSource() { public float get(boolean p) { return 0; } } : source;
    }
    public static synchronized void setTimeSource(World world, TimeSource source) {
        if (source == null) worldSources.remove(world); else worldSources.put(world, source);
    }
    private static float clampPartial(float value) { return Math.max(0, Math.min(1, value)); }

    public interface PartialTickSource { float get(boolean pausedAware); }
    public interface TimeSource { int getTicks(); float getPartialTicks(); }
}
