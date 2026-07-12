package net.createmod.ponder.foundation.instruction;

import net.createmod.ponder.api.element.AnimatedOverlayElement;
import net.createmod.ponder.api.element.AnimatedSceneElement;
import net.createmod.ponder.api.element.PonderElement;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;

public final class HideAllInstruction extends TickingInstruction {
    private final EnumFacing direction;
    public HideAllInstruction(int ticks, EnumFacing direction) { super(false, ticks); this.direction = direction; }
    @Override protected void firstTick(PonderScene scene) {
        for (PonderElement element : scene.getElements()) {
            if (element instanceof AnimatedSceneElement) {
                ((AnimatedSceneElement) element).forceApplyFade(1);
                ((AnimatedSceneElement) element).setFadeVec(direction == null ? Vec3d.ZERO : new Vec3d(direction.getDirectionVec()).scale(.5));
            } else if (element instanceof AnimatedOverlayElement) ((AnimatedOverlayElement) element).setFade(1);
            else element.setVisible(false);
        }
    }
    @Override protected void tickRunning(PonderScene scene, int elapsed, float progress) {
        final float fade = (1 - progress) * (1 - progress);
        scene.forEach(AnimatedSceneElement.class, element -> element.setFade(fade));
        scene.forEach(AnimatedOverlayElement.class, element -> element.setFade(fade));
    }
    @Override protected void finish(PonderScene scene) { scene.forEach(PonderElement.class, element -> element.setVisible(false)); }
}
