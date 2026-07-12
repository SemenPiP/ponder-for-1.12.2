package net.createmod.ponder.foundation.instruction;

import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.ParrotElement;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.util.math.Vec3d;

public final class AnimateParrotInstruction extends TickingInstruction {
    private final ElementLink<ParrotElement> link;
    private final Vec3d delta;
    private final boolean rotation;
    private ParrotElement element;
    private Vec3d start;

    private AnimateParrotInstruction(ElementLink<ParrotElement> link, Vec3d delta, boolean rotation, int duration) {
        super(false, duration); this.link = link; this.delta = delta; this.rotation = rotation;
    }

    public static AnimateParrotInstruction rotate(ElementLink<ParrotElement> link, Vec3d delta, int duration) {
        return new AnimateParrotInstruction(link, delta, true, duration);
    }

    public static AnimateParrotInstruction move(ElementLink<ParrotElement> link, Vec3d delta, int duration) {
        return new AnimateParrotInstruction(link, delta, false, duration);
    }

    @Override protected void firstTick(PonderScene scene) {
        element = scene.resolve(link);
        start = element == null ? Vec3d.ZERO : rotation ? element.getRotation() : element.getPositionOffset();
    }

    @Override protected void tickRunning(PonderScene scene, int elapsed, float progress) {
        if (element == null) return;
        Vec3d value = start.add(delta.scale(progress * progress * (3 - 2 * progress)));
        if (rotation) element.setRotation(value, progress >= 1); else element.setPositionOffset(value, progress >= 1);
    }
}
