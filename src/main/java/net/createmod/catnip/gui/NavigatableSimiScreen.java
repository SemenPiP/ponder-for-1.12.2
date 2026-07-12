package net.createmod.catnip.gui;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public abstract class NavigatableSimiScreen extends AbstractSimiScreen {
    protected int depthPointX;
    protected int depthPointY;

    public void centerScalingOn(int x, int y) {
        depthPointX = x;
        depthPointY = y;
    }

    public boolean isEquivalentTo(NavigatableSimiScreen other) {
        return other != null && getClass() == other.getClass();
    }

    /** Copies this requested screen's transition origin into an equivalent history entry. */
    public void shareContextWith(NavigatableSimiScreen other) {
        if (other == null || other == this) return;
        other.depthPointX = depthPointX;
        other.depthPointY = depthPointY;
    }

    protected String getBreadcrumbTitle() {
        return getClass().getSimpleName();
    }
}
