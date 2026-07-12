package net.createmod.catnip.lang;

import java.util.Locale;

public final class Lang {
    private Lang() {}
    public static String asId(String name) { return name.toLowerCase(Locale.ROOT); }
    public static String nonPluralId(String name) {
        String id = asId(name); return id.endsWith("s") && id.length() > 1 ? id.substring(0, id.length() - 1) : id;
    }
    public static LangBuilder builder(String namespace) { return new LangBuilder(namespace); }
}
