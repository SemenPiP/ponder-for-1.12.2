package net.createmod.ponder.foundation.instruction;

import net.createmod.ponder.api.element.PonderElement;
import net.createmod.ponder.foundation.PonderScene;

public final class ShowElementInstruction extends TickingInstruction {
    private final PonderElement element;
    private final boolean removeWhenFinished;

    public ShowElementInstruction(PonderElement element, int duration, boolean removeWhenFinished) {
        super(false, Math.max(1, duration));
        if (element == null) throw new IllegalArgumentException("Element may not be null");
        this.element = element;
        this.removeWhenFinished = removeWhenFinished;
    }

    @Override protected void firstTick(PonderScene scene) {
        element.setVisible(true);
        scene.addElement(element);
    }

    @Override protected void finish(PonderScene scene) {
        if (removeWhenFinished) {
            element.setVisible(false);
            scene.removeElement(element);
        }
    }
}
