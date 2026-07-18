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
    private static final Map<String, String> LOCAL = new LinkedHashMap<String, String>();
    private static final Map<String, String> SERVER = new LinkedHashMap<String, String>();

    private ScriptSharedText() {
    }

    @ZenMethod
    public static synchronized void register(String key, String defaultText) {
        key = ScriptWorldBuilder.requiredText(key, "shared text key");
        defaultText = ScriptWorldBuilder.requiredText(defaultText, "shared text");
        validateEntry(key, defaultText);
        if (LOCAL.size() >= ScriptSceneSnapshot.MAX_SHARED_TEXT)
            throw new IllegalStateException("Ponder shared text limit reached: "
                + ScriptSceneSnapshot.MAX_SHARED_TEXT);
        if (LOCAL.containsKey(key)) throw new IllegalArgumentException("Duplicate shared text key: " + key);
        LOCAL.put(key, defaultText);
    }

    static synchronized Map<String, String> snapshot() {
        Map<String, String> effective = new LinkedHashMap<String, String>(LOCAL);
        effective.putAll(SERVER);
        return immutable(effective);
    }

    static synchronized Map<String, String> localSnapshot() {
        return immutable(LOCAL);
    }

    static synchronized Map<String, String> serverSnapshot() {
        return immutable(SERVER);
    }

    static synchronized void replaceServer(Map<String, String> values) {
        Map<String, String> replacement = validate(values);
        SERVER.clear();
        SERVER.putAll(replacement);
    }

    static synchronized void clearServer() {
        SERVER.clear();
    }

    private static Map<String, String> validate(Map<String, String> values) {
        if (values == null || values.size() > ScriptSceneSnapshot.MAX_SHARED_TEXT)
            throw new IllegalArgumentException("Invalid server Ponder shared text collection");
        Map<String, String> replacement = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            validateEntry(entry.getKey(), entry.getValue());
            if (replacement.put(entry.getKey(), entry.getValue()) != null)
                throw new IllegalArgumentException("Duplicate server shared text key: " + entry.getKey());
        }
        return replacement;
    }

    private static void validateEntry(String key, String value) {
        if (key == null || key.trim().isEmpty() || key.length() > 256)
            throw new IllegalArgumentException("Shared text key is missing or too long");
        if (value == null || value.length() > ScriptSceneSnapshot.MAX_TEXT_LENGTH)
            throw new IllegalArgumentException("Shared text value is missing or too long: " + key);
    }

    private static Map<String, String> immutable(Map<String, String> source) {
        return Collections.unmodifiableMap(new LinkedHashMap<String, String>(source));
    }
}
