package net.createmod.ponder.api.element;

import net.createmod.ponder.foundation.PonderScene;

public interface AnimatedOverlayElement extends PonderOverlayElement {
    void setFade(float fade);

    float getFade(float partialTicks);

    @Override
    default void render(PonderScene scene, int mouseX, int mouseY, float partialTicks) {
        render(scene, mouseX, mouseY, partialTicks, getFade(partialTicks));
    }

    void render(PonderScene scene, int mouseX, int mouseY, float partialTicks, float fade);
}
