package net.createmod.catnip.config.ui.entries;

import com.google.common.base.Predicate;

import net.minecraftforge.fml.client.config.ConfigGuiType;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.GuiConfigEntries;
import net.minecraftforge.fml.client.config.IConfigElement;

/** Numeric Forge entry with live parsing and range validation. */
public class NumberEntry extends ValueEntry<Number> {
    public NumberEntry(GuiConfig owningScreen, GuiConfigEntries owningEntryList,
                       IConfigElement configElement) {
        super(owningScreen, owningEntryList, configElement);
        textFieldValue.setValidator(new Predicate<String>() {
            @Override public boolean apply(String input) {
                return isIntermediate(input) || isValidNumber(input);
            }
        });
        validateCurrent();
    }

    @Override
    public void keyTyped(char eventChar, int eventKey) {
        super.keyTyped(eventChar, eventKey);
        validateCurrent();
    }

    @Override
    public boolean saveConfigElement() {
        validateCurrent();
        return super.saveConfigElement();
    }

    private void validateCurrent() {
        isValidValue = isValidNumber(textFieldValue.getText());
    }

    private boolean isValidNumber(String input) {
        if (input == null || input.trim().isEmpty()) return false;
        try {
            double value;
            if (configElement.getType() == ConfigGuiType.INTEGER) {
                value = Integer.parseInt(input);
            } else {
                value = Double.parseDouble(input);
                if (Double.isNaN(value) || Double.isInfinite(value)) return false;
            }
            Double min = parseBound(configElement.getMinValue());
            Double max = parseBound(configElement.getMaxValue());
            return (min == null || value >= min) && (max == null || value <= max);
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static boolean isIntermediate(String input) {
        return input != null && (input.isEmpty() || "-".equals(input) || "+".equals(input)
            || ".".equals(input) || "-.".equals(input) || "+.".equals(input));
    }

    private static Double parseBound(Object value) {
        return value == null || String.valueOf(value).isEmpty()
            ? null : Double.valueOf(String.valueOf(value));
    }
}
