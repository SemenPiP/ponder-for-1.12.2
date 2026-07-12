package net.createmod.ponder.foundation.instruction;

import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderScene;

public abstract class WorldModifyInstruction extends PonderInstruction {
    private final Selection selection;
    protected WorldModifyInstruction(Selection selection) { this.selection = selection.copy(); }
    @Override public boolean isComplete() { return true; }
    @Override public void tick(PonderScene scene) {
        runModification(selection.copy(), scene);
        if (needsRedraw()) scene.forEach(WorldSectionElement.class, WorldSectionElement::queueRedraw);
    }
    protected abstract void runModification(Selection selection, PonderScene scene);
    protected abstract boolean needsRedraw();
}
