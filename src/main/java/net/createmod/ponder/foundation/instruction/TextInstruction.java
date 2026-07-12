package net.createmod.ponder.foundation.instruction;

import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.TextWindowElement;

public final class TextInstruction extends FadeInOutInstruction {
    private final TextWindowElement element; private final Selection outline;
    public TextInstruction(TextWindowElement element, int duration) { this(element, duration, null); }
    public TextInstruction(TextWindowElement element, int duration, Selection outline) {
        super(duration); this.element = element; this.outline = outline == null ? null : outline.copy();
    }
    @Override protected void show(PonderScene scene) { scene.addElement(element); element.setVisible(true); }
    @Override protected void hide(PonderScene scene) { element.setVisible(false); }
    @Override protected void applyFade(PonderScene scene, float fade) {
        element.setVisible(fade > 0);
        if (outline != null && fade > 0) outline.makeOutline(scene.getOutliner(), element).alpha(fade);
    }
}
