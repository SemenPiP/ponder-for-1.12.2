package net.createmod.ponder.script;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import crafttweaker.annotations.ZenRegister;
import net.minecraft.util.ResourceLocation;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.ponder.TagRegistry")
public final class ScriptTagRegistry {
    private static final Map<ResourceLocation, ScriptTagDefinition> ZS_LOCAL =
        new LinkedHashMap<ResourceLocation, ScriptTagDefinition>();
    private static final Map<ResourceLocation, ScriptTagDefinition> JSON_LOCAL =
        new LinkedHashMap<ResourceLocation, ScriptTagDefinition>();
    private static final Map<ResourceLocation, ScriptTagDefinition> SERVER =
        new LinkedHashMap<ResourceLocation, ScriptTagDefinition>();

    private ScriptTagRegistry() {
    }

    @ZenMethod
    public static ScriptTagBuilder create(String id, String icon, String title, String description) {
        return new ScriptTagBuilder(id, icon, title, description);
    }

    static synchronized void register(ScriptTagDefinition definition) {
        if (ZS_LOCAL.size() + JSON_LOCAL.size() >= ScriptSceneSnapshot.MAX_TAGS)
            throw new IllegalStateException("Ponder script tag limit reached: "
                + ScriptSceneSnapshot.MAX_TAGS);
        if (ZS_LOCAL.containsKey(definition.id) || JSON_LOCAL.containsKey(definition.id))
            throw new IllegalArgumentException("Duplicate Ponder script tag id: " + definition.id);
        ZS_LOCAL.put(definition.id, definition);
    }

    static synchronized Collection<ScriptTagDefinition> snapshot() {
        Map<ResourceLocation, ScriptTagDefinition> effective =
            localMap();
        effective.putAll(SERVER);
        return immutable(effective.values());
    }

    static synchronized Collection<ScriptTagDefinition> localSnapshot() {
        return immutable(localMap().values());
    }

    static synchronized Collection<ScriptTagDefinition> serverSnapshot() {
        return immutable(SERVER.values());
    }

    static synchronized void replaceServer(Collection<ScriptTagDefinition> definitions) {
        Map<ResourceLocation, ScriptTagDefinition> replacement = validate(definitions);
        SERVER.clear();
        SERVER.putAll(replacement);
    }

    static synchronized void clearServer() {
        SERVER.clear();
    }

    static synchronized Collection<ScriptTagDefinition> jsonSnapshot() {
        return immutable(JSON_LOCAL.values());
    }

    static synchronized boolean containsZenTag(ResourceLocation tagId) {
        return ZS_LOCAL.containsKey(tagId);
    }

    static synchronized void replaceJson(Collection<ScriptTagDefinition> definitions) {
        Map<ResourceLocation, ScriptTagDefinition> replacement =
            validateJson(definitions);
        JSON_LOCAL.clear();
        JSON_LOCAL.putAll(replacement);
    }

    private static Map<ResourceLocation, ScriptTagDefinition> validate(
            Collection<ScriptTagDefinition> definitions) {
        if (definitions == null || definitions.size() > ScriptSceneSnapshot.MAX_TAGS)
            throw new IllegalArgumentException("Invalid server Ponder tag collection");
        Map<ResourceLocation, ScriptTagDefinition> replacement =
            new LinkedHashMap<ResourceLocation, ScriptTagDefinition>();
        int componentCount = 0;
        for (ScriptTagDefinition definition : definitions) {
            if (definition == null || replacement.put(definition.id, definition) != null)
                throw new IllegalArgumentException("Duplicate or null server Ponder tag");
            componentCount += definition.components.size();
            if (componentCount > ScriptSceneSnapshot.MAX_TAG_COMPONENTS)
                throw new IllegalArgumentException("Server Ponder tags contain too many component associations");
        }
        return replacement;
    }

    private static Map<ResourceLocation, ScriptTagDefinition> validateJson(
            Collection<ScriptTagDefinition> definitions) {
        if (definitions == null || definitions.size() + ZS_LOCAL.size() > ScriptSceneSnapshot.MAX_TAGS)
            throw new IllegalArgumentException("Invalid local JSON Ponder tag collection");
        Map<ResourceLocation, ScriptTagDefinition> replacement =
            new LinkedHashMap<ResourceLocation, ScriptTagDefinition>();
        int componentCount = 0;
        for (ScriptTagDefinition definition : definitions) {
            if (definition == null)
                throw new IllegalArgumentException("Local JSON Ponder tag may not be null");
            if (ZS_LOCAL.containsKey(definition.id))
                throw new IllegalArgumentException("JSON tag conflicts with ZenScript tag " + definition.id);
            if (replacement.put(definition.id, definition) != null)
                throw new IllegalArgumentException("Duplicate local JSON Ponder tag " + definition.id);
            componentCount += definition.components.size();
            if (componentCount > ScriptSceneSnapshot.MAX_TAG_COMPONENTS)
                throw new IllegalArgumentException("Local JSON Ponder tags contain too many component associations");
        }
        return replacement;
    }

    private static Map<ResourceLocation, ScriptTagDefinition> localMap() {
        Map<ResourceLocation, ScriptTagDefinition> merged =
            new LinkedHashMap<ResourceLocation, ScriptTagDefinition>(ZS_LOCAL);
        merged.putAll(JSON_LOCAL);
        return merged;
    }

    private static Collection<ScriptTagDefinition> immutable(
            Collection<ScriptTagDefinition> definitions) {
        return Collections.unmodifiableCollection(new ArrayList<ScriptTagDefinition>(definitions));
    }
}
