package net.createmod.ponder.foundation.instruction;

import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.util.math.Vec3d;

public final class MovePoiInstruction extends CallbackInstruction {
    public MovePoiInstruction(final Vec3d point) { super(scene -> scene.setPointOfInterest(point)); }
}
