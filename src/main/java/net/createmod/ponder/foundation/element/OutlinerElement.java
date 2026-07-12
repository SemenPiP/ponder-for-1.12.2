package net.createmod.ponder.foundation.element;

import java.util.function.Function;

import net.createmod.catnip.outliner.Outline.OutlineParams;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.ponder.foundation.PonderScene;

/**
 * Scene-owned adapter for Catnip's TTL-based outliner.
 *
 * <p>The outline supplier is refreshed while this element is visible or fading in. During a
 * fade-out it is deliberately no longer refreshed, allowing the 1.12 Outliner to perform its
 * normal removal fade instead of popping the outline out immediately.</p>
 */
public class OutlinerElement extends AnimatedSceneElementBase {
    private final Function<Outliner, OutlineParams> outlinerCall;
    private int overrideColor = -1;

    public OutlinerElement(Function<Outliner, OutlineParams> outlinerCall) {
        if (outlinerCall == null) {
            throw new IllegalArgumentException("An outliner callback is required");
        }
        this.outlinerCall = outlinerCall;
    }

    @Override
    public void tick(PonderScene scene) {
        if (fade < 1 / 16f || previousFade > fade) {
            return;
        }
        OutlineParams params = outlinerCall.apply(scene.getOutliner());
        if (params == null) {
            throw new IllegalStateException("The outliner callback did not return outline parameters");
        }
        if (overrideColor != -1) {
            params.colored(overrideColor);
        }
    }

    public void setColor(int overrideColor) {
        this.overrideColor = overrideColor;
    }

    @Override
    public Object captureState() {
        return new State(super.captureState(), overrideColor);
    }

    @Override
    public void restoreState(Object value) {
        if (!(value instanceof State)) {
            super.restoreState(value);
            return;
        }
        State state = (State) value;
        super.restoreState(state.animation);
        overrideColor = state.overrideColor;
    }

    private static final class State {
        private final Object animation;
        private final int overrideColor;

        private State(Object animation, int overrideColor) {
            this.animation = animation;
            this.overrideColor = overrideColor;
        }
    }
}
