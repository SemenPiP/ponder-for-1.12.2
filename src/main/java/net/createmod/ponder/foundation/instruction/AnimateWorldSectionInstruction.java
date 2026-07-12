package net.createmod.ponder.foundation.instruction;

import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.util.math.Vec3d;

public final class AnimateWorldSectionInstruction extends TickingInstruction {
    private final ElementLink<WorldSectionElement> link;
    private final Vec3d delta;
    private final boolean rotation;
    private WorldSectionElement element;
    private Vec3d start;

    private AnimateWorldSectionInstruction(ElementLink<WorldSectionElement> link, Vec3d delta,
                                           boolean rotation, int duration) {
        super(false, duration);
        this.link = link;
        this.delta = delta;
        this.rotation = rotation;
    }

    public static AnimateWorldSectionInstruction rotate(ElementLink<WorldSectionElement> link,
                                                        Vec3d rotation, int duration) {
        return new AnimateWorldSectionInstruction(link, rotation, true, duration);
    }

    public static AnimateWorldSectionInstruction move(ElementLink<WorldSectionElement> link,
                                                      Vec3d offset, int duration) {
        return new AnimateWorldSectionInstruction(link, offset, false, duration);
    }

    @Override protected void firstTick(PonderScene scene) {
        element = scene.resolve(link);
        start = element == null ? Vec3d.ZERO : rotation ? element.getAnimatedRotation() : element.getAnimatedOffset();
    }

    @Override protected void tickRunning(PonderScene scene, int elapsed, float progress) {
        if (element == null) return;
        double eased = progress * progress * (3 - 2 * progress);
        Vec3d value = start.add(delta.scale(eased));
        if (rotation) element.setAnimatedRotation(value, progress >= 1);
        else element.setAnimatedOffset(value, progress >= 1);
    }
}
