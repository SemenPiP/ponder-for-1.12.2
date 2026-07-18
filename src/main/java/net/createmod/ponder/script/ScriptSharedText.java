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
    private static final Map<String, String> ZS_LOCAL = new LinkedHashMap<String, String>();
    private static final Map<String, String> JSON_LOCAL = new LinkedHashMap<String, String>();
    private static final Map<String, String> SERVER = new LinkedHashMap<String, String>();

    private ScriptSharedText() {
    }

    @ZenMethod
    public static synchronized void register(String key, String defaultText) {
        key = ScriptWorldBuilder.requiredText(key, "shared text key");
        defaultText = ScriptWorldBuilder.requiredText(defaultText, "shared text");
        validateEntry(key, defaultText);
        if (ZS_LOCAL.size() + JSON_LOCAL.size() >= ScriptSceneSnapshot.MAX_SHARED_TEXT)
            throw new IllegalStateException("Ponder shared text limit reached: "
                + ScriptSceneSnapshot.MAX_SHARED_TEXT);
        if (ZS_LOCAL.containsKey(key) || JSON_LOCAL.containsKey(key))
            throw new IllegalArgumentException("Duplicate shared text key: " + key);
        ZS_LOCAL.put(key, defaultText);
    }

    static synchronized Map<String, String> snapshot() {
        Map<String, String> effective = localMap();
        effective.putAll(SERVER);
        return immutable(effective);
    }

    static synchronized Map<String, String> localSnapshot() {
        return immutable(localMap());
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

    static synchronized Map<String, String> jsonSnapshot() {
        return immutable(JSON_LOCAL);
    }

    static synchronized boolean containsZenKey(String key) {
        return ZS_LOCAL.containsKey(key);
    }

    static synchronized void replaceJson(Map<String, String> values) {
        if (values == null || values.size() + ZS_LOCAL.size() > ScriptSceneSnapshot.MAX_SHARED_TEXT)
            throw new IllegalArgumentException("Invalid local JSON Ponder shared text collection");
        Map<String, String> replacement = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            validateEntry(entry.getKey(), entry.getValue());
            if (ZS_LOCAL.containsKey(entry.getKey()))
                throw new IllegalArgumentException(
                    "JSON shared text conflicts with ZenScript key " + entry.getKey());
            if (replacement.put(entry.getKey(), entry.getValue()) != null)
                throw new IllegalArgumentException("Duplicate local JSON shared text key: " + entry.getKey());
        }
        JSON_LOCAL.clear();
        JSON_LOCAL.putAll(replacement);
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

    private static Map<String, String> localMap() {
        Map<String, String> merged = new LinkedHashMap<String, String>(ZS_LOCAL);
        merged.putAll(JSON_LOCAL);
        return merged;
    }
}
