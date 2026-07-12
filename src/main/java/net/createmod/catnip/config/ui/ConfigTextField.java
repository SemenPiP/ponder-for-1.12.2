package net.createmod.catnip.config.ui;

import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Text field used by config filtering; clicking outside releases focus. */
@SideOnly(Side.CLIENT)
public class ConfigTextField extends HintableTextFieldWidget {
    public ConfigTextField(int id, FontRenderer font, int x, int y, int width, int height) {
        super(id, font, x, y, width, height);
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        boolean inside = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        if (!inside) setFocused(false);
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }
}
