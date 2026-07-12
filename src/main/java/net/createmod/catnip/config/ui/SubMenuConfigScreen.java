package net.createmod.catnip.config.ui;

import javax.annotation.Nullable;

import net.createmod.catnip.config.ConfigPath;
import net.createmod.catnip.config.ConfigType;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** GuiConfig view for one 1.12 Configuration side. Forge supplies editing, undo and reset. */
@SideOnly(Side.CLIENT)
public class SubMenuConfigScreen extends ConfigScreen {
    private final ConfigType type;

    public SubMenuConfigScreen(@Nullable GuiScreen parent, String modId, ConfigType type) {
        super(parent, ConfigHelper.getElements(modId, type), modId,
            ConfigHelper.toHumanReadable(modId) + " - " + ConfigHelper.toHumanReadable(type.name()));
        this.type = type;
    }

    public static SubMenuConfigScreen find(ConfigPath path) {
        if (path == null) throw new IllegalArgumentException("path");
        return new SubMenuConfigScreen(null, path.getModId(), path.getType());
    }

    public ConfigType getConfigType() {
        return type;
    }
}
