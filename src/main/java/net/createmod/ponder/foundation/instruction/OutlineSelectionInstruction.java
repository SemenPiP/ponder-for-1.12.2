package net.createmod.ponder.foundation.instruction;

import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderScene;

public final class OutlineSelectionInstruction extends TickingInstruction {
    private final PonderPalette color; private final Object slot; private final Selection selection;
    public OutlineSelectionInstruction(PonderPalette color, Object slot, Selection selection, int ticks) {
        super(false, ticks); this.color = color; this.slot = slot; this.selection = selection.copy();
    }
    @Override protected void tickRunning(PonderScene scene, int elapsed, float progress) {
        selection.makeOutline(scene.getOutliner(), slot).lineWidth(1 / 16f).colored(color.getColor());
    }
}
