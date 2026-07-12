package net.createmod.ponder.foundation.instruction;

import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.util.math.Vec3d;

public final class LineInstruction extends TickingInstruction {
    private final PonderPalette color; private final Vec3d start, end; private final boolean big;
    public LineInstruction(PonderPalette color, Vec3d start, Vec3d end, int ticks, boolean big) {
        super(false, ticks); this.color = color; this.start = start; this.end = end; this.big = big;
    }
    @Override protected void tickRunning(PonderScene scene, int elapsed, float progress) {
        scene.getOutliner().showLine(start, start, end).lineWidth(big ? 1 / 8f : 1 / 16f).colored(color.getColor());
    }
}
