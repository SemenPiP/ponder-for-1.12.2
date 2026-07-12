package net.createmod.catnip.lang;

import java.text.NumberFormat;
import java.util.Locale;

public final class LangNumberFormat {
    private static volatile NumberFormat format = create(Locale.ROOT);
    private LangNumberFormat() {}
    public static synchronized void update(Locale locale) { format = create(locale == null ? Locale.ROOT : locale); }
    public static String format(double value) {
        if (Math.abs(value) < 1.0E-12) value = 0;
        NumberFormat local = (NumberFormat) format.clone();
        return local.format(value).replace('\u00A0', ' ');
    }
    private static NumberFormat create(Locale locale) {
        NumberFormat value = NumberFormat.getNumberInstance(locale);
        value.setMaximumFractionDigits(2); value.setMinimumFractionDigits(0); value.setGroupingUsed(true); return value;
    }
}
