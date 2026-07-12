package net.createmod.catnip.gui;

import java.io.IOException;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public abstract class AbstractSimiScreen extends GuiScreen {
    @Override
    public final void drawScreen(int mouseX, int mouseY, float partialTicks) {
        renderWindowBackground(mouseX, mouseY, partialTicks);
        renderWindow(mouseX, mouseY, partialTicks);
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderWindowForeground(mouseX, mouseY, partialTicks);
    }

    protected void renderWindowBackground(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
    }

    protected abstract void renderWindow(int mouseX, int mouseY, float partialTicks);

    protected void renderWindowForeground(int mouseX, int mouseY, float partialTicks) {}

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_BACK && ScreenOpener.openPreviousScreen()) {
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
