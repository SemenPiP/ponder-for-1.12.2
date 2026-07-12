package net.createmod.ponder.foundation.instruction;

import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

public final class HighlightValueBoxInstruction extends TickingInstruction {
    private final Vec3d point, expansion;
    public HighlightValueBoxInstruction(Vec3d point, Vec3d expansion, int duration) {
        super(false, duration); this.point = point; this.expansion = expansion;
    }
    @Override protected void tickRunning(PonderScene scene, int elapsed, float progress) {
        AxisAlignedBB box = new AxisAlignedBB(point.x, point.y, point.z, point.x, point.y, point.z);
        if (elapsed > 1) box = box.grow(expansion.x, expansion.y, expansion.z);
        scene.getOutliner().chaseAABB(point, box).lineWidth(1 / 15f).colored(PonderPalette.WHITE.getColor());
    }
}
