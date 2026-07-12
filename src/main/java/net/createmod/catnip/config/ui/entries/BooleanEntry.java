package net.createmod.catnip.config.ui.entries;

import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.GuiConfigEntries;
import net.minecraftforge.fml.client.config.IConfigElement;

/** Boolean cycle button with Forge-native change tracking. */
public class BooleanEntry extends GuiConfigEntries.ButtonEntry {
    private final boolean beforeValue;
    private boolean currentValue;

    public BooleanEntry(GuiConfig owningScreen, GuiConfigEntries owningEntryList,
                        IConfigElement configElement) {
        super(owningScreen, owningEntryList, configElement);
        beforeValue = Boolean.parseBoolean(String.valueOf(configElement.get()));
        currentValue = beforeValue;
        updateValueButtonText();
    }

    @Override public void updateValueButtonText() {
        btnValue.displayString = I18n.format(Boolean.toString(currentValue));
        btnValue.packedFGColour = currentValue ? 0x55ff55 : 0xff5555;
    }
    @Override public void valueButtonPressed(int slotIndex) { if (enabled()) currentValue = !currentValue; }
    @Override public boolean isDefault() {
        return currentValue == Boolean.parseBoolean(String.valueOf(configElement.getDefault()));
    }
    @Override public void setToDefault() {
        if (enabled()) { currentValue = Boolean.parseBoolean(String.valueOf(configElement.getDefault())); updateValueButtonText(); }
    }
    @Override public boolean isChanged() { return currentValue != beforeValue; }
    @Override public void undoChanges() { if (enabled()) { currentValue = beforeValue; updateValueButtonText(); } }
    @Override public boolean saveConfigElement() {
        if (!enabled() || !isChanged()) return false;
        configElement.set(currentValue);
        return configElement.requiresMcRestart();
    }
    @Override public Boolean getCurrentValue() { return currentValue; }
    @Override public Boolean[] getCurrentValues() { return new Boolean[] {currentValue}; }
}
