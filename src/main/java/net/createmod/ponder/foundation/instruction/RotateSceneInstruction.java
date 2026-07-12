package net.createmod.ponder.foundation.instruction;

import net.createmod.ponder.foundation.PonderScene;

public final class RotateSceneInstruction extends CallbackInstruction {
    public RotateSceneInstruction(final float xRotation, final float yRotation, final boolean relative) {
        super(scene -> {
            scene.setCameraPitch(relative ? scene.getCameraPitch() + xRotation : xRotation);
            scene.setCameraYaw(relative ? scene.getCameraYaw() + yRotation : yRotation);
        });
    }
}
