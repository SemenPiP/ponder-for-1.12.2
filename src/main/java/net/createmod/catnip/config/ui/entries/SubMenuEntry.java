package net.createmod.catnip.config.ui.entries;

import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.GuiConfigEntries;
import net.minecraftforge.fml.client.config.IConfigElement;

/**
 * Forge 1.12.2 category entry used by Catnip's configuration bridge.
 * Forge's implementation supplies navigation, change tracking, reset and save behavior.
 */
public class SubMenuEntry extends GuiConfigEntries.CategoryEntry {
    public SubMenuEntry(GuiConfig owningScreen, GuiConfigEntries owningEntryList,
                        IConfigElement configElement) {
        super(owningScreen, owningEntryList, configElement);
    }
}
