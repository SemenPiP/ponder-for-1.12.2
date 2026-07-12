package net.createmod.ponder.foundation.instruction;

import java.util.function.Consumer;

import net.createmod.ponder.foundation.PonderScene;

public class CallbackInstruction extends PonderInstruction {
    private final Consumer<PonderScene> callback;
    private boolean complete;

    public CallbackInstruction(Consumer<PonderScene> callback) {
        if (callback == null)
            throw new IllegalArgumentException("Instruction callback may not be null");
        this.callback = callback;
    }

    @Override public void reset(PonderScene scene) { complete = false; }
    @Override public boolean isComplete() { return complete; }
    @Override public void tick(PonderScene scene) { if (!complete) { callback.accept(scene); complete = true; } }
    @Override public Object captureState() { return Boolean.valueOf(complete); }
    @Override public void restoreState(Object state) { complete = Boolean.TRUE.equals(state); }
}
