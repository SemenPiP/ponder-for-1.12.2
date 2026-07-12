package net.createmod.ponder.foundation.instruction;

import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.util.math.AxisAlignedBB;

public final class ChaseAABBInstruction extends TickingInstruction {
    private final AxisAlignedBB bounds;
    private final Object slot;
    private final PonderPalette color;

    public ChaseAABBInstruction(PonderPalette color, Object slot, AxisAlignedBB bounds, int ticks) {
        super(false, ticks); this.color = color; this.slot = slot; this.bounds = bounds;
    }
    @Override protected void tickRunning(PonderScene scene, int elapsed, float progress) {
        scene.getOutliner().chaseAABB(slot, bounds).lineWidth(1 / 16f).colored(color.getColor());
    }
}
