package net.createmod.catnip.config.ui.entries;

import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.GuiConfigEntries;
import net.minecraftforge.fml.client.config.IConfigElement;

/** String value entry using Forge's native editing, undo, reset and save behavior. */
public class StringEntry extends ValueEntry<String> {
    public StringEntry(GuiConfig owningScreen, GuiConfigEntries owningEntryList,
                       IConfigElement configElement) {
        super(owningScreen, owningEntryList, configElement);
    }
}
