package net.createmod.ponder.foundation.ui;

import net.createmod.catnip.gui.NavigatableSimiScreen;
import net.minecraft.client.gui.Gui;

public abstract class AbstractPonderScreen extends NavigatableSimiScreen {
    @Override
    protected void renderWindowBackground(int mouseX,int mouseY,float partialTicks) {
        Gui.drawRect(0,0,width,height,0xff101215);
        Gui.drawRect(0,0,width,34,0xff171b20);
        Gui.drawRect(0,height-38,width,height,0xff171b20);
    }
}
