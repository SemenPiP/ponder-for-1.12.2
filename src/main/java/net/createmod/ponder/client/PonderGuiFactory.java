package net.createmod.ponder.client;

import java.util.Collections;
import java.util.Set;

import net.createmod.catnip.config.ConfigType;
import net.createmod.catnip.config.ui.ConfigHelper;
import net.createmod.ponder.Ponder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Exposes Catnip's Forge Configuration-backed client settings in the mod list. */
@SideOnly(Side.CLIENT)
public final class PonderGuiFactory implements IModGuiFactory {
    @Override
    public void initialize(Minecraft minecraftInstance) {
    }

    @Override
    public boolean hasConfigGui() {
        return true;
    }

    @Override
    public GuiScreen createConfigGui(GuiScreen parentScreen) {
        return new GuiConfig(parentScreen,
            ConfigHelper.getElements(Ponder.MOD_ID, ConfigType.CLIENT),
            Ponder.MOD_ID, false, false, "Ponder Configuration") {
            @Override
            public void onGuiClosed() {
                super.onGuiClosed();
                ConfigHelper.saveAndReload(Ponder.MOD_ID);
            }
        };
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return Collections.emptySet();
    }
}
