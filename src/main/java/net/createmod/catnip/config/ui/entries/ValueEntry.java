package net.createmod.catnip.config.ui.entries;

import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.GuiConfigEntries;
import net.minecraftforge.fml.client.config.IConfigElement;

/** Forge text-entry base retained under Catnip's public class name. */
public abstract class ValueEntry<T> extends GuiConfigEntries.StringEntry {
    protected ValueEntry(GuiConfig owningScreen, GuiConfigEntries owningEntryList,
                         IConfigElement configElement) {
        super(owningScreen, owningEntryList, configElement);
    }

    public GuiTextField getTextField() {
        return textFieldValue;
    }

    public String getSerializedValue() {
        return textFieldValue.getText();
    }
}
