package net.createmod.ponder.foundation.ui;

import java.util.function.BiConsumer;

import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.createmod.ponder.foundation.PonderChapter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ChapterLabel extends AbstractSimiWidget {
    private final PonderChapter chapter;
    private final PonderButton button;
    public ChapterLabel(final PonderChapter chapter, int x, int y, final BiConsumer<Integer, Integer> onClick) {
        super(0, x, y, 175, 38, ""); this.chapter = chapter;
        button = new PonderButton(0, x + 4, y + 4, 30, 30, PonderButton.Icon.INDEX)
            .withCallback(() -> onClick.accept(x, y));
    }
    @Override public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!visible) return; Gui.drawRect(x, y + 2, x + width, y + height - 2, 0xaa171b20);
        button.drawButton(mc, mouseX, mouseY, partialTicks);
        mc.fontRenderer.drawString(chapter.getTitle(), x + 44, y + 15, 0xffd9e1e8);
    }
    @Override public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) { return button.mousePressed(mc, mouseX, mouseY); }
}
