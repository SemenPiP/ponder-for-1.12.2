package net.createmod.catnip.config.ui;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.createmod.catnip.config.ConfigPath;
import net.createmod.catnip.gui.ScreenOpener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Resolves the bounded command payload into a real Forge-era config screen. */
@SideOnly(Side.CLIENT)
public final class ConfigScreenOpener {
    private static final Logger LOGGER = LogManager.getLogger("CatnipConfigScreen");

    private ConfigScreenOpener() {
    }

    public static void open(String request) {
        Minecraft minecraft = Minecraft.getMinecraft();
        GuiScreen parent = minecraft.currentScreen;
        String modId;
        try {
            modId = resolveModId(request);
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("Ignoring invalid config screen request: {}", request, exception);
            ScreenOpener.open(new ConfigModListScreen(parent));
            return;
        }
        if (modId == null) {
            ScreenOpener.open(new ConfigModListScreen(parent));
            return;
        }
        if (!ConfigHelper.hasAnyConfig(modId)) {
            LOGGER.warn("No Catnip config is registered for {}", modId);
            ScreenOpener.open(new ConfigModListScreen(parent));
            return;
        }
        ScreenOpener.open(new BaseConfigScreen(parent, modId).searchForConfigSpecs());
    }

    @Nullable
    static String resolveModId(String request) {
        String value = request == null ? "" : request.trim();
        if (value.isEmpty()) return null;
        if (value.indexOf(':') >= 0) return ConfigPath.parse(value).getModId();
        if (!value.matches("[A-Za-z0-9_-]+"))
            throw new IllegalArgumentException("Invalid mod id: " + value);
        return value;
    }
}
