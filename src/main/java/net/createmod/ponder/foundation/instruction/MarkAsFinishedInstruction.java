package net.createmod.ponder.foundation.instruction;

import net.createmod.ponder.foundation.PonderScene;

public final class MarkAsFinishedInstruction extends CallbackInstruction {
    public MarkAsFinishedInstruction() {
        super(scene -> scene.setFinished(true));
    }
}
