package net.createmod.catnip.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

public class BoxWidget extends AbstractSimiWidget {
    private int background = 0xaa101010;
    private int border = 0xff606060;

    public BoxWidget(int id, int x, int y, int width, int height) {
        super(id, x, y, width, height, "");
    }

    public BoxWidget colors(int background, int border) {
        this.background = background;
        this.border = border;
        return this;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!visible) {
            return;
        }
        hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        Gui.drawRect(x, y, x + width, y + height, hovered ? brighten(background) : background);
        Gui.drawRect(x, y, x + width, y + 1, border);
        Gui.drawRect(x, y + height - 1, x + width, y + height, border);
        Gui.drawRect(x, y, x + 1, y + height, border);
        Gui.drawRect(x + width - 1, y, x + width, y + height, border);
    }

    private static int brighten(int color) {
        int r = Math.min(255, (color >> 16 & 255) + 20);
        int g = Math.min(255, (color >> 8 & 255) + 20);
        int b = Math.min(255, (color & 255) + 20);
        return color & 0xff000000 | r << 16 | g << 8 | b;
    }
}
