package net.createmod.ponder.api.element;

import net.createmod.ponder.foundation.PonderScene;

public interface PonderOverlayElement extends PonderElement {
    void render(PonderScene scene, int mouseX, int mouseY, float partialTicks);
}
