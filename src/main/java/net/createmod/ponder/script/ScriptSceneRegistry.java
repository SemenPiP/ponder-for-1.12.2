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
import net.createmod.ponder.api.diagnostic.PonderDiagnosticIssue;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticSeverity;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticView;
import net.minecraft.util.ResourceLocation;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.ponder.SceneRegistry")
public final class ScriptSceneRegistry {
    public static final int MAX_SCENES = 2048;
    private static final Map<ResourceLocation, ScriptSceneDefinition> ZS_LOCAL =
        new LinkedHashMap<ResourceLocation, ScriptSceneDefinition>();
    private static final Map<ResourceLocation, ScriptSceneDefinition> JSON_LOCAL =
        new LinkedHashMap<ResourceLocation, ScriptSceneDefinition>();
    private static final Map<ResourceLocation, ScriptSceneDefinition> SERVER =
        new LinkedHashMap<ResourceLocation, ScriptSceneDefinition>();
    private static final Set<ScriptSceneBuilder> PENDING =
        Collections.newSetFromMap(new IdentityHashMap<ScriptSceneBuilder, Boolean>());
    private static final List<PonderDiagnosticIssue> REGISTRATION_ISSUES =
        new ArrayList<PonderDiagnosticIssue>();

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
            ZS_LOCAL.remove(id);
        }
    }

    @ZenMethod
    public static void removeComponent(String componentId) {
        ResourceLocation component = parseId(componentId, "component id");
        synchronized (ScriptSceneRegistry.class) {
            ZS_LOCAL.values().removeIf(definition -> definition.getComponent().equals(component));
        }
    }

    static synchronized void register(ScriptSceneDefinition definition) {
        if (ZS_LOCAL.size() + JSON_LOCAL.size() >= MAX_SCENES)
            throw new IllegalStateException("Ponder script scene limit reached: " + MAX_SCENES);
        if (ZS_LOCAL.containsKey(definition.getSceneId())
            || JSON_LOCAL.containsKey(definition.getSceneId()))
            throw new IllegalArgumentException("Duplicate Ponder script scene id: " + definition.getSceneId());
        ZS_LOCAL.put(definition.getSceneId(), definition);
        CraftTweakerAPI.logInfo("Registered Ponder scene " + definition.getSceneId()
            + " for " + definition.getComponent());
    }

    static synchronized void registrationAttempted(ScriptSceneBuilder builder) {
        PENDING.remove(builder);
    }

    static synchronized void recordRegistrationFailure(ResourceLocation sceneId, String source,
                                                       RuntimeException failure) {
        String reason = failure.getMessage() == null || failure.getMessage().trim().isEmpty()
            ? failure.getClass().getSimpleName() : failure.getMessage();
        String message = "Failed to register Ponder scene " + sceneId + ": " + reason
            + (source == null || source.isEmpty() ? "" : " at " + source);
        addRegistrationIssue(new PonderDiagnosticIssue("registration.script_failed",
            PonderDiagnosticSeverity.ERROR, message, sceneId, -1));
    }

    static synchronized void recordStructureFailure(ResourceLocation sceneId,
                                                    ResourceLocation structureId,
                                                    RuntimeException failure) {
        String reason = failure.getMessage() == null || failure.getMessage().trim().isEmpty()
            ? failure.getClass().getSimpleName() : failure.getMessage();
        addRegistrationIssue(new PonderDiagnosticIssue("structure.provider_failed",
            PonderDiagnosticSeverity.ERROR, "Skipped Ponder scene " + sceneId
                + ": structure provider failed for " + structureId + ": " + reason,
            sceneId, -1));
    }

    public static synchronized void reportUnregisteredBuilders() {
        for (ScriptSceneBuilder builder : PENDING) {
            String source = builder.getSourceDescription();
            String message = "Ponder scene builder was not registered: " + builder.getSceneId()
                + (source == null ? "" : " at " + source);
            CraftTweakerAPI.logError(message);
            addRegistrationIssue(new PonderDiagnosticIssue("registration.script_unregistered",
                PonderDiagnosticSeverity.ERROR, message, builder.getSceneId(), -1));
        }
        PENDING.clear();
    }

    public static synchronized List<PonderDiagnosticIssue> drainRegistrationIssues() {
        List<PonderDiagnosticIssue> result =
            new ArrayList<PonderDiagnosticIssue>(REGISTRATION_ISSUES);
        REGISTRATION_ISSUES.clear();
        return result;
    }

    public static synchronized Collection<ScriptSceneDefinition> effectiveScenes() {
        Map<ResourceLocation, ScriptSceneDefinition> merged =
            localMap();
        merged.putAll(SERVER);
        return Collections.unmodifiableCollection(new ArrayList<ScriptSceneDefinition>(merged.values()));
    }

    public static synchronized List<ScriptSceneDefinition> localSnapshot(boolean includeClientOnly) {
        List<ScriptSceneDefinition> result = new ArrayList<ScriptSceneDefinition>();
        for (ScriptSceneDefinition definition : localMap().values())
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

    public static synchronized void replaceServerSnapshotAndReload(ScriptSceneSnapshot.Decoded decoded) {
        replaceServerSnapshotAndReload(decoded, new Runnable() {
            @Override
            public void run() {
                net.createmod.ponder.foundation.PonderIndex.reload();
            }
        });
    }

    static synchronized void replaceServerSnapshotAndReload(ScriptSceneSnapshot.Decoded decoded,
                                                            Runnable reload) {
        if (decoded == null)
            throw new IllegalArgumentException("Server Ponder snapshot is required");
        if (reload == null)
            throw new IllegalArgumentException("Ponder snapshot reload action is required");
        Map<ResourceLocation, ScriptSceneDefinition> replacement =
            validateServerScenes(decoded.scenes);
        Map<ResourceLocation, ScriptSceneDefinition> previousScenes =
            new LinkedHashMap<ResourceLocation, ScriptSceneDefinition>(SERVER);
        Collection<ScriptTagDefinition> previousTags = ScriptTagRegistry.serverSnapshot();
        Map<String, String> previousSharedText = ScriptSharedText.serverSnapshot();
        try {
            SERVER.clear();
            SERVER.putAll(replacement);
            ScriptTagRegistry.replaceServer(decoded.tags);
            ScriptSharedText.replaceServer(decoded.sharedText);
            reload.run();
        } catch (RuntimeException failure) {
            SERVER.clear();
            SERVER.putAll(previousScenes);
            ScriptTagRegistry.replaceServer(previousTags);
            ScriptSharedText.replaceServer(previousSharedText);
            throw failure;
        }
    }

    public static synchronized void clearServerScenesAndReload() {
        Map<ResourceLocation, ScriptSceneDefinition> previousScenes =
            new LinkedHashMap<ResourceLocation, ScriptSceneDefinition>(SERVER);
        Collection<ScriptTagDefinition> previousTags = ScriptTagRegistry.serverSnapshot();
        Map<String, String> previousSharedText = ScriptSharedText.serverSnapshot();
        try {
            SERVER.clear();
            ScriptTagRegistry.clearServer();
            ScriptSharedText.clearServer();
            net.createmod.ponder.foundation.PonderIndex.reload();
        } catch (RuntimeException failure) {
            SERVER.clear();
            SERVER.putAll(previousScenes);
            ScriptTagRegistry.replaceServer(previousTags);
            ScriptSharedText.replaceServer(previousSharedText);
            throw failure;
        }
    }

    public static synchronized List<ScriptSceneDefinition> serverSnapshot() {
        return Collections.unmodifiableList(new ArrayList<ScriptSceneDefinition>(SERVER.values()));
    }

    public static synchronized ScriptSceneDefinition find(PonderDiagnosticView view, ResourceLocation sceneId) {
        if (sceneId == null)
            return null;
        if (view == PonderDiagnosticView.SERVER)
            return SERVER.get(sceneId);
        if (view == PonderDiagnosticView.LOCAL)
            return localMap().get(sceneId);
        ScriptSceneDefinition server = SERVER.get(sceneId);
        return server == null ? localMap().get(sceneId) : server;
    }

    static synchronized List<ScriptSceneDefinition> jsonSnapshot() {
        return Collections.unmodifiableList(new ArrayList<ScriptSceneDefinition>(JSON_LOCAL.values()));
    }

    static synchronized void replaceJsonScenes(Collection<ScriptSceneDefinition> definitions) {
        Map<ResourceLocation, ScriptSceneDefinition> replacement =
            validateJsonScenes(definitions);
        JSON_LOCAL.clear();
        JSON_LOCAL.putAll(replacement);
    }

    static synchronized boolean containsZenScene(ResourceLocation sceneId) {
        return ZS_LOCAL.containsKey(sceneId);
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

    private static Map<ResourceLocation, ScriptSceneDefinition> validateJsonScenes(
            Collection<ScriptSceneDefinition> definitions) {
        if (definitions == null || definitions.size() + ZS_LOCAL.size() > MAX_SCENES)
            throw new IllegalArgumentException("Invalid local JSON Ponder scene collection");
        Map<ResourceLocation, ScriptSceneDefinition> replacement =
            new LinkedHashMap<ResourceLocation, ScriptSceneDefinition>();
        for (ScriptSceneDefinition definition : definitions) {
            if (definition == null)
                throw new IllegalArgumentException("Local JSON Ponder scene may not be null");
            ResourceLocation sceneId = definition.getSceneId();
            if (ZS_LOCAL.containsKey(sceneId))
                throw new IllegalArgumentException("JSON scene conflicts with ZenScript scene " + sceneId);
            if (replacement.put(sceneId, definition) != null)
                throw new IllegalArgumentException("Duplicate local JSON Ponder scene id: " + sceneId);
        }
        return replacement;
    }

    private static Map<ResourceLocation, ScriptSceneDefinition> localMap() {
        Map<ResourceLocation, ScriptSceneDefinition> merged =
            new LinkedHashMap<ResourceLocation, ScriptSceneDefinition>(ZS_LOCAL);
        merged.putAll(JSON_LOCAL);
        return merged;
    }

    static synchronized void recordJsonIssue(String code, PonderDiagnosticSeverity severity,
                                             String message, ResourceLocation sceneId,
                                             int instructionIndex) {
        addRegistrationIssue(new PonderDiagnosticIssue(code, severity, message,
            sceneId, instructionIndex));
    }

    private static void addRegistrationIssue(PonderDiagnosticIssue issue) {
        for (PonderDiagnosticIssue existing : REGISTRATION_ISSUES)
            if (existing.getCode().equals(issue.getCode())
                && existing.getMessage().equals(issue.getMessage())
                && java.util.Objects.equals(existing.getSceneId(), issue.getSceneId()))
                return;
        REGISTRATION_ISSUES.add(issue);
    }

    static ResourceLocation parseId(String value, String label) {
        if (value == null || value.trim().isEmpty())
            throw new IllegalArgumentException(label + " is required");
        if (value.length() > 256)
            throw new IllegalArgumentException(label + " exceeds 256 characters");
        return new ResourceLocation(value);
    }
}
