package net.createmod.ponder.foundation.instruction;

import java.util.function.Consumer;

import net.createmod.ponder.foundation.PonderScene;

/** A resettable timeline action. One instance may be replayed and restored repeatedly. */
public abstract class PonderInstruction {
    public boolean isBlocking() {
        return false;
    }

    public int getDuration() {
        return 0;
    }

    public void reset(PonderScene scene) {
    }

    public void onScheduled(PonderScene scene) {
    }

    public abstract boolean isComplete();

    public abstract void tick(PonderScene scene);

    public Object captureState() {
        return null;
    }

    public void restoreState(Object state) {
    }

    public static PonderInstruction simple(Consumer<PonderScene> callback) {
        return new CallbackInstruction(callback);
    }
}
