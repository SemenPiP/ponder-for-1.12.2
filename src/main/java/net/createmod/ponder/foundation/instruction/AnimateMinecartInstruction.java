package net.createmod.ponder.foundation.instruction;

import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.MinecartElement;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.util.math.Vec3d;

public final class AnimateMinecartInstruction extends TickingInstruction {
    private final ElementLink<MinecartElement> link;
    private final Vec3d delta;
    private final boolean rotation;
    private MinecartElement element;
    private Vec3d start;

    private AnimateMinecartInstruction(ElementLink<MinecartElement> link, Vec3d delta, boolean rotation, int duration) {
        super(false, duration); this.link = link; this.delta = delta; this.rotation = rotation;
    }
    public static AnimateMinecartInstruction rotate(ElementLink<MinecartElement> link, float delta, int duration) {
        return new AnimateMinecartInstruction(link, new Vec3d(0, delta, 0), true, duration);
    }
    public static AnimateMinecartInstruction move(ElementLink<MinecartElement> link, Vec3d delta, int duration) {
        return new AnimateMinecartInstruction(link, delta, false, duration);
    }
    @Override protected void firstTick(PonderScene scene) {
        element = scene.resolve(link);
        start = element == null ? Vec3d.ZERO : rotation ? element.getRotation() : element.getPositionOffset();
    }
    @Override protected void tickRunning(PonderScene scene, int elapsed, float progress) {
        if (element == null) return;
        Vec3d value = start.add(delta.scale(progress * progress * (3 - 2 * progress)));
        if (rotation) element.setRotation((float) value.y, progress >= 1); else element.setPositionOffset(value, progress >= 1);
    }
}
