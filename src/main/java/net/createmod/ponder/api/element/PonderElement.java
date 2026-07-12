package net.createmod.ponder.api.element;

import net.createmod.ponder.foundation.PonderScene;

public interface PonderElement {
    default void whileSkipping(PonderScene scene) {
    }

    default void tick(PonderScene scene) {
    }

    default void reset(PonderScene scene) {
    }

    boolean isVisible();

    void setVisible(boolean visible);

    /** Memento hook used by deterministic keyframe restore. */
    default Object captureState() {
        return Boolean.valueOf(isVisible());
    }

    default void restoreState(Object state) {
        if (state instanceof Boolean)
            setVisible(((Boolean) state).booleanValue());
    }
}
