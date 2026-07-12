package net.createmod.catnip.config.ui;

import java.io.IOException;

import javax.annotation.Nullable;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Searchable list of loaded mods; only mods with registered Catnip configs can be opened. */
@SideOnly(Side.CLIENT)
public class ConfigModListScreen extends GuiScreen {
    @Nullable private final GuiScreen parent;
    private ConfigScreenList list;
    private ConfigTextField search;

    public ConfigModListScreen(@Nullable GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        search = new ConfigTextField(20, fontRenderer, width / 2 - 150, 20, 300, 18);
        search.setHint("Search mods");
        list = new ConfigScreenList(mc, this, width, height, 45, height - 35, 28);
        buttonList.add(new GuiButton(2000, width / 2 - 50, height - 27, 100, 20, "Done"));
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 2000) mc.displayGuiScreen(parent);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
            return;
        }
        if (search.textboxKeyTyped(typedChar, keyCode)) {
            list.setFilter(search.getText());
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        search.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        list.handleMouseInput();
    }

    @Override public void updateScreen() { search.updateCursorCounter(); }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, "Mod Configuration", width / 2, 7, 0xffffff);
        list.drawScreen(mouseX, mouseY, partialTicks);
        search.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override public boolean doesGuiPauseGame() { return true; }
}
