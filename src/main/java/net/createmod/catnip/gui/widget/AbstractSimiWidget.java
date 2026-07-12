package net.createmod.catnip.gui.widget;

import net.minecraft.client.gui.GuiButton;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class AbstractSimiWidget extends GuiButton {
    private String tooltip;

    public AbstractSimiWidget(int id, int x, int y, int width, int height, String label) {
        super(id, x, y, width, height, label);
    }

    public AbstractSimiWidget withTooltip(String tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    public String getTooltip() {
        return tooltip;
    }
}
