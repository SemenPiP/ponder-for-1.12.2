package net.createmod.catnip.config.ui;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Forge GuiConfig-backed replacement for Catnip's modern configuration screen base. */
@SideOnly(Side.CLIENT)
public class ConfigScreen extends GuiConfig {
    public ConfigScreen(@Nullable GuiScreen parent, List<IConfigElement> elements,
                        String modId, String title) {
        super(parent, elements, modId, modId, false, false, title);
    }

    public static String toHumanReadable(String key) {
        return ConfigHelper.toHumanReadable(key);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        ConfigHelper.saveAndReload(modID);
    }
}
