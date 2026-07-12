package net.createmod.catnip.gui;

import net.minecraft.client.renderer.RenderHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface ILightingSettings {
    void applyLighting();

    ILightingSettings DEFAULT_3D = new ILightingSettings() {
        @Override
        public void applyLighting() {
            RenderHelper.enableGUIStandardItemLighting();
        }
    };

    ILightingSettings DEFAULT_FLAT = new ILightingSettings() {
        @Override
        public void applyLighting() {
            RenderHelper.disableStandardItemLighting();
        }
    };
}
