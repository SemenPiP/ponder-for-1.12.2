package net.createmod.catnip.config.ui;

import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Top-level screen listing the client, common and server configurations of one mod. */
@SideOnly(Side.CLIENT)
public class BaseConfigScreen extends ConfigScreen {
    private final String configuredModId;

    public BaseConfigScreen(@Nullable GuiScreen parent, String modId) {
        super(parent, ConfigHelper.getRootElements(modId), modId,
            ConfigHelper.toHumanReadable(modId) + " Configuration");
        configuredModId = modId;
    }

    public BaseConfigScreen searchForConfigSpecs() {
        needsRefresh = true;
        return this;
    }

    public String getConfiguredModId() {
        return configuredModId;
    }
}
