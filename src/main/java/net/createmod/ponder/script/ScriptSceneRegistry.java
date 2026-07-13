package net.createmod.ponder.script;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.IdentityHashMap;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import net.minecraft.util.ResourceLocation;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.ponder.SceneRegistry")
public final class ScriptSceneRegistry {
    public static final int MAX_SCENES = 2048;
    private static final Map<ResourceLocation, ScriptSceneDefinition> LOCAL =
        new LinkedHashMap<ResourceLocation, ScriptSceneDefinition>();
    private static final Map<ResourceLocation, ScriptSceneDefinition> SERVER =
        new LinkedHashMap<ResourceLocation, ScriptSceneDefinition>();
    private static final Set<ScriptSceneBuilder> PENDING =
        Collections.newSetFromMap(new IdentityHashMap<ScriptSceneBuilder, Boolean>());

    private ScriptSceneRegistry() {
    }

    @ZenMethod
    public static ScriptSceneBuilder create(String componentId, String sceneId, String title, String structureId) {
        ScriptSceneBuilder builder = new ScriptSceneBuilder(componentId, sceneId, title, structureId);
        synchronized (ScriptSceneRegistry.class) {
            PENDING.add(builder);
        }
        return builder;
    }

    @ZenMethod
    public static void removeScene(String sceneId) {
        ResourceLocation id = parseId(sceneId, "scene id");
        synchronized (ScriptSceneRegistry.class) {
            LOCAL.remove(id);
        }
    }

    @ZenMethod
    public static void removeComponent(String componentId) {
        ResourceLocation component = parseId(componentId, "component id");
        synchronized (ScriptSceneRegistry.class) {
            LOCAL.values().removeIf(definition -> definition.getComponent().equals(component));
        }
    }

    static synchronized void register(ScriptSceneDefinition definition) {
        if (LOCAL.size() >= MAX_SCENES)
            throw new IllegalStateException("Ponder script scene limit reached: " + MAX_SCENES);
        if (LOCAL.containsKey(definition.getSceneId()))
            throw new IllegalArgumentException("Duplicate Ponder script scene id: " + definition.getSceneId());
        LOCAL.put(definition.getSceneId(), definition);
        CraftTweakerAPI.logInfo("Registered Ponder scene " + definition.getSceneId()
            + " for " + definition.getComponent());
    }

    static synchronized void registrationAttempted(ScriptSceneBuilder builder) {
        PENDING.remove(builder);
    }

    public static synchronized void reportUnregisteredBuilders() {
        for (ScriptSceneBuilder builder : PENDING) {
            String source = builder.getSourceDescription();
            CraftTweakerAPI.logError("Ponder scene builder was not registered: " + builder.getSceneId()
                + (source == null ? "" : " at " + source));
        }
        PENDING.clear();
    }

    public static synchronized Collection<ScriptSceneDefinition> effectiveScenes() {
        Map<ResourceLocation, ScriptSceneDefinition> merged =
            new LinkedHashMap<ResourceLocation, ScriptSceneDefinition>(LOCAL);
        merged.putAll(SERVER);
        return Collections.unmodifiableCollection(new ArrayList<ScriptSceneDefinition>(merged.values()));
    }

    public static synchronized List<ScriptSceneDefinition> localSnapshot(boolean includeClientOnly) {
        List<ScriptSceneDefinition> result = new ArrayList<ScriptSceneDefinition>();
        for (ScriptSceneDefinition definition : LOCAL.values())
            if (includeClientOnly || !definition.isClientOnly()) result.add(definition);
        return Collections.unmodifiableList(result);
    }

    public static synchronized void replaceServerScenes(Collection<ScriptSceneDefinition> definitions) {
        Map<ResourceLocation, ScriptSceneDefinition> replacement = validateServerScenes(definitions);
        SERVER.clear();
        SERVER.putAll(replacement);
    }

    public static synchronized void clearServerScenes() {
        SERVER.clear();
    }

    public static synchronized void replaceServerScenesAndReload(Collection<ScriptSceneDefinition> definitions) {
        Map<ResourceLocation, ScriptSceneDefinition> replacement = validateServerScenes(definitions);
        Map<ResourceLocation, ScriptSceneDefinition> previous =
            new LinkedHashMap<ResourceLocation, ScriptSceneDefinition>(SERVER);
        SERVER.clear();
        SERVER.putAll(replacement);
        try {
            net.createmod.ponder.foundation.PonderIndex.reload();
        } catch (RuntimeException failure) {
            SERVER.clear();
            SERVER.putAll(previous);
            throw failure;
        }
    }

    public static synchronized void clearServerScenesAndReload() {
        replaceServerScenesAndReload(Collections.<ScriptSceneDefinition>emptyList());
    }

    public static synchronized List<ScriptSceneDefinition> serverSnapshot() {
        return Collections.unmodifiableList(new ArrayList<ScriptSceneDefinition>(SERVER.values()));
    }

    private static Map<ResourceLocation, ScriptSceneDefinition> validateServerScenes(
            Collection<ScriptSceneDefinition> definitions) {
        if (definitions == null || definitions.size() > MAX_SCENES)
            throw new IllegalArgumentException("Invalid server Ponder scene collection");
        Map<ResourceLocation, ScriptSceneDefinition> replacement =
            new LinkedHashMap<ResourceLocation, ScriptSceneDefinition>();
        for (ScriptSceneDefinition definition : definitions) {
            if (definition == null)
                throw new IllegalArgumentException("Server Ponder scene may not be null");
            if (replacement.put(definition.getSceneId(), definition) != null)
                throw new IllegalArgumentException("Duplicate server Ponder scene id: " + definition.getSceneId());
        }
        return replacement;
    }

    static ResourceLocation parseId(String value, String label) {
        if (value == null || value.trim().isEmpty())
            throw new IllegalArgumentException(label + " is required");
        if (value.length() > 256)
            throw new IllegalArgumentException(label + " exceeds 256 characters");
        return new ResourceLocation(value);
    }
}
