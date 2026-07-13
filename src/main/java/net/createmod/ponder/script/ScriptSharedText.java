package net.createmod.ponder.script;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.ponder.SharedText")
public final class ScriptSharedText {
    private static final Map<String, String> TEXT = new LinkedHashMap<String, String>();

    private ScriptSharedText() {
    }

    @ZenMethod
    public static synchronized void register(String key, String defaultText) {
        key = ScriptWorldBuilder.requiredText(key, "shared text key");
        defaultText = ScriptWorldBuilder.requiredText(defaultText, "shared text");
        if (TEXT.containsKey(key)) throw new IllegalArgumentException("Duplicate shared text key: " + key);
        TEXT.put(key, defaultText);
    }

    static synchronized Map<String, String> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<String, String>(TEXT));
    }
}
