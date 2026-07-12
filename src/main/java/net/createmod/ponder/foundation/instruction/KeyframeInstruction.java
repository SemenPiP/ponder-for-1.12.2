package net.createmod.ponder.foundation.instruction;

import net.createmod.ponder.foundation.PonderScene;

public final class KeyframeInstruction extends PonderInstruction {
    public static final KeyframeInstruction IMMEDIATE = new KeyframeInstruction(0);
    public static final KeyframeInstruction DELAYED = new KeyframeInstruction(6);
    private final int offset;

    public KeyframeInstruction(int offset) {
        this.offset = Math.max(0, offset);
    }

    @Override public boolean isComplete() { return true; }
    @Override public void tick(PonderScene scene) { }
    @Override public void onScheduled(PonderScene scene) { scene.declareKeyframe(scene.getBuildCursor() + offset); }
}
