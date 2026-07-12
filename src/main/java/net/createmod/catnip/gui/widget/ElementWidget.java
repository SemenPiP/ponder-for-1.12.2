package net.createmod.catnip.gui.widget;

import net.createmod.catnip.gui.element.ScreenElement;
import net.minecraft.client.Minecraft;

public class ElementWidget extends AbstractSimiWidget {
    private final ScreenElement element;

    public ElementWidget(int id, int x, int y, int width, int height, ScreenElement element) {
        super(id, x, y, width, height, "");
        if (element == null) {
            throw new IllegalArgumentException("element");
        }
        this.element = element;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!visible) {
            return;
        }
        hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        element.render(x + width / 2, y + height / 2, enabled ? 1f : .4f);
    }
}
