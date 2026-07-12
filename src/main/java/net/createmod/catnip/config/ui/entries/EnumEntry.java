package net.createmod.catnip.config.ui.entries;

import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.GuiConfigEntries;
import net.minecraftforge.fml.client.config.IConfigElement;

/** Button entry cycling through a property's declared valid values. */
public class EnumEntry extends GuiConfigEntries.ButtonEntry {
    private final String[] values;
    private final String[] displayValues;
    private final int beforeIndex;
    private final int defaultIndex;
    private int currentIndex;

    public EnumEntry(GuiConfig owningScreen, GuiConfigEntries owningEntryList,
                     IConfigElement configElement) {
        super(owningScreen, owningEntryList, configElement);
        values = configElement.getValidValues();
        if (values == null || values.length == 0)
            throw new IllegalArgumentException("EnumEntry requires valid values for " + configElement.getName());
        String[] configuredDisplays = configElement.getValidValuesDisplay();
        displayValues = configuredDisplays != null && configuredDisplays.length == values.length
            ? configuredDisplays.clone() : values.clone();
        beforeIndex = find(String.valueOf(configElement.get()), 0);
        defaultIndex = find(String.valueOf(configElement.getDefault()), 0);
        currentIndex = beforeIndex;
        updateValueButtonText();
    }

    @Override public void updateValueButtonText() {
        btnValue.displayString = I18n.format(displayValues[currentIndex]);
    }
    @Override public void valueButtonPressed(int slotIndex) {
        if (enabled()) currentIndex = (currentIndex + 1) % values.length;
    }
    @Override public boolean isDefault() { return currentIndex == defaultIndex; }
    @Override public void setToDefault() { if (enabled()) { currentIndex = defaultIndex; updateValueButtonText(); } }
    @Override public boolean isChanged() { return currentIndex != beforeIndex; }
    @Override public void undoChanges() { if (enabled()) { currentIndex = beforeIndex; updateValueButtonText(); } }
    @Override public boolean saveConfigElement() {
        if (!enabled() || !isChanged()) return false;
        configElement.set(values[currentIndex]);
        return configElement.requiresMcRestart();
    }
    @Override public String getCurrentValue() { return values[currentIndex]; }
    @Override public String[] getCurrentValues() { return new String[] {values[currentIndex]}; }

    private int find(String value, int fallback) {
        for (int i = 0; i < values.length; i++) if (values[i].equalsIgnoreCase(value)) return i;
        return fallback;
    }
}
