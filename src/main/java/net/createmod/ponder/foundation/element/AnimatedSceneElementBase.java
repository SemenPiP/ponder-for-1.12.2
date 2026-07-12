package net.createmod.ponder.foundation.element;

import net.createmod.ponder.api.element.AnimatedSceneElement;
import net.minecraft.util.math.Vec3d;

public abstract class AnimatedSceneElementBase extends PonderElementBase implements AnimatedSceneElement {
    protected float previousFade = 1;
    protected float fade = 1;
    protected Vec3d fadeVector = Vec3d.ZERO;

    @Override public void forceApplyFade(float value) {
        fade = previousFade = clamp(value);
        visible = fade > 0;
    }
    @Override public void setFade(float value) {
        previousFade = fade;
        fade = clamp(value);
        visible = fade > 0 || previousFade > 0;
    }
    @Override public void setFadeVec(Vec3d value) { fadeVector = value == null ? Vec3d.ZERO : value; }
    protected float getFade(float partialTicks) { return previousFade + (fade - previousFade) * partialTicks; }
    private static float clamp(float value) { return Math.max(0, Math.min(1, value)); }

    @Override public Object captureState() {
        return new AnimationState(visible, previousFade, fade, fadeVector);
    }
    @Override public void restoreState(Object value) {
        if (!(value instanceof AnimationState)) { super.restoreState(value); return; }
        AnimationState state = (AnimationState) value;
        visible = state.visible; previousFade = state.previousFade; fade = state.fade; fadeVector = state.fadeVector;
    }
    protected static final class AnimationState {
        final boolean visible; final float previousFade, fade; final Vec3d fadeVector;
        AnimationState(boolean visible, float previousFade, float fade, Vec3d fadeVector) {
            this.visible = visible; this.previousFade = previousFade; this.fade = fade; this.fadeVector = fadeVector;
        }
    }
}
