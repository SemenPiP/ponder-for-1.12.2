package net.createmod.ponder.foundation.instruction;

import net.createmod.ponder.api.element.AnimatedSceneElement;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;

public final class FadeOutOfSceneInstruction<T extends AnimatedSceneElement> extends TickingInstruction {
    private final ElementLink<T> link;
    private final EnumFacing direction;
    private T element;

    public FadeOutOfSceneInstruction(int ticks, EnumFacing direction, ElementLink<T> link) {
        super(false, ticks);
        this.link = link;
        this.direction = direction == null ? EnumFacing.DOWN : direction;
    }

    @Override protected void firstTick(PonderScene scene) {
        element = scene.resolve(link);
        if (element != null) element.setFadeVec(new Vec3d(direction.getDirectionVec()));
    }

    @Override protected void tickRunning(PonderScene scene, int elapsed, float progress) {
        if (element != null) element.setFade(1 - progress * progress * (3 - 2 * progress));
    }

    @Override protected void finish(PonderScene scene) {
        if (element != null) element.setVisible(false);
    }
}
