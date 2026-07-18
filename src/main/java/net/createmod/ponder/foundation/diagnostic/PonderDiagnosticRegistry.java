package net.createmod.ponder.foundation.diagnostic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.createmod.ponder.Ponder;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticIssue;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticContext;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticContributor;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticContributors;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticSink;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticSeverity;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticSnapshot;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticView;
import net.createmod.ponder.api.diagnostic.PonderSceneDiagnostic;
import net.createmod.ponder.api.diagnostic.PonderSceneSource;
import net.createmod.ponder.api.diagnostic.PonderStructureDependency;
import net.createmod.ponder.api.diagnostic.PonderStructureDependencyStatus;
import net.createmod.ponder.api.registration.StoryBoardEntry;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.PonderStoryBoardEntry;
import net.createmod.ponder.foundation.registration.PonderSceneRegistry;
import net.createmod.ponder.foundation.structure.PonderStructure;
import net.createmod.ponder.script.ScriptPonderPlugin;
import net.createmod.ponder.script.ScriptMissingStructures;
import net.createmod.ponder.script.ScriptSceneDefinition;
import net.createmod.ponder.script.ScriptSceneRegistry;
import net.createmod.ponder.script.ScriptSourceMetadata;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLCommonHandler;

public final class PonderDiagnosticRegistry {
    private static final Comparator<PonderSceneDiagnostic> ORDER =
        new Comparator<PonderSceneDiagnostic>() {
            @Override
            public int compare(PonderSceneDiagnostic left, PonderSceneDiagnostic right) {
                String leftId = left.getSceneId() == null ? left.getEntryKey() : left.getSceneId().toString();
                String rightId = right.getSceneId() == null ? right.getEntryKey() : right.getSceneId().toString();
                return leftId.compareTo(rightId);
            }
        };

    private static long generation;
    private static volatile PonderDiagnosticSnapshot local =
        PonderDiagnosticSnapshot.empty(PonderDiagnosticView.LOCAL);
    private static volatile PonderDiagnosticSnapshot server =
        PonderDiagnosticSnapshot.empty(PonderDiagnosticView.SERVER);
    private static volatile PonderDiagnosticSnapshot effective =
        PonderDiagnosticSnapshot.empty(PonderDiagnosticView.EFFECTIVE);
    private static volatile Map<String, List<PonderScene.ScheduledInstructionDiagnostic>> javaTimelines =
        Collections.emptyMap();
    private static volatile List<PonderStructureDependency> localDependencies = Collections.emptyList();
    private static volatile List<PonderStructureDependency> serverDependencies = Collections.emptyList();
    private static volatile List<PonderStructureDependency> effectiveDependencies = Collections.emptyList();

    private PonderDiagnosticRegistry() {
    }

    public static synchronized void rebuild(PonderSceneRegistry registry) {
        long nextGeneration = ++generation;
        PonderDiagnosticNotices.beginGeneration(nextGeneration);
        List<PonderSceneDiagnostic> localScenes = new ArrayList<PonderSceneDiagnostic>();
        List<PonderSceneDiagnostic> serverScenes = new ArrayList<PonderSceneDiagnostic>();
        Map<String, List<PonderScene.ScheduledInstructionDiagnostic>> nextJavaTimelines =
            new LinkedHashMap<String, List<PonderScene.ScheduledInstructionDiagnostic>>();
        int javaIndex = 0;
        for (StoryBoardEntry entry : registry.snapshotEntries()) {
            if (entry instanceof PonderStoryBoardEntry
                && ScriptPonderPlugin.class.getName().equals(
                    ((PonderStoryBoardEntry) entry).getPluginClass()))
                continue;
            localScenes.add(buildJava(registry, entry, javaIndex++, nextJavaTimelines));
        }
        for (ScriptSceneDefinition definition : ScriptSceneRegistry.localSnapshot(true))
            localScenes.add(buildScript(registry, definition,
                ScriptSourceMetadata.isBuiltin(definition.getSourceDescription())
                    ? PonderSceneSource.BUILTIN_ZS : PonderSceneSource.LOCAL_ZS));
        boolean serverProcess = isServerProcess();
        if (serverProcess) {
            for (ScriptSceneDefinition definition : ScriptSceneRegistry.localSnapshot(false))
                serverScenes.add(buildScript(registry, definition,
                    ScriptSourceMetadata.isBuiltin(definition.getSourceDescription())
                        ? PonderSceneSource.BUILTIN_ZS : PonderSceneSource.LOCAL_ZS));
        } else {
            for (ScriptSceneDefinition definition : ScriptSceneRegistry.serverSnapshot())
                serverScenes.add(buildScript(registry, definition, PonderSceneSource.SERVER_SNAPSHOT));
        }

        localScenes = markDuplicateIds(localScenes);
        serverScenes = markDuplicateIds(serverScenes);
        Collections.sort(localScenes, ORDER);
        Collections.sort(serverScenes, ORDER);

        List<PonderSceneDiagnostic> effectiveScenes;
        if (serverProcess) {
            effectiveScenes = new ArrayList<PonderSceneDiagnostic>(localScenes);
        } else {
            ClientViews merged = mergeClientViews(localScenes, serverScenes);
            localScenes = merged.local;
            effectiveScenes = merged.effective;
        }
        Collections.sort(effectiveScenes, ORDER);

        long now = System.currentTimeMillis();
        local = snapshot(PonderDiagnosticView.LOCAL, nextGeneration, now, localScenes);
        server = snapshot(PonderDiagnosticView.SERVER, nextGeneration, now, serverScenes);
        effective = snapshot(PonderDiagnosticView.EFFECTIVE, nextGeneration, now, effectiveScenes);
        ContributionResult localContribution = contribute(local);
        ContributionResult serverContribution = contribute(server);
        ContributionResult effectiveContribution = contribute(effective);
        local = localContribution.snapshot;
        server = serverContribution.snapshot;
        effective = effectiveContribution.snapshot;
        localDependencies = localContribution.dependencies;
        serverDependencies = serverContribution.dependencies;
        effectiveDependencies = effectiveContribution.dependencies;
        javaTimelines = immutableTimelines(nextJavaTimelines);
        for (PonderDiagnosticIssue issue : effective.getIssues())
            PonderDiagnosticNotices.record(issue);
        for (PonderDiagnosticIssue issue : ScriptMissingStructures.drainDiagnosticIssues())
            recordRuntimeIssue(issue);
        for (PonderDiagnosticIssue issue : ScriptSceneRegistry.drainRegistrationIssues())
            recordRuntimeIssue(issue);
    }

    public static synchronized void recordRuntimeIssue(PonderDiagnosticIssue issue) {
        if (issue == null)
            return;
        local = withIssue(local, issue);
        server = withIssue(server, issue);
        effective = withIssue(effective, issue);
        PonderDiagnosticNotices.record(issue);
    }

    public static PonderDiagnosticSnapshot snapshot(PonderDiagnosticView view) {
        if (view == PonderDiagnosticView.LOCAL)
            return local;
        if (view == PonderDiagnosticView.SERVER)
            return server;
        return effective;
    }

    public static List<PonderStructureDependency> structureDependencies(PonderDiagnosticView view) {
        if (view == PonderDiagnosticView.LOCAL)
            return localDependencies;
        if (view == PonderDiagnosticView.SERVER)
            return serverDependencies;
        return effectiveDependencies;
    }

    public static List<PonderScene.ScheduledInstructionDiagnostic> javaTimeline(
            PonderDiagnosticView view, String entryKey) {
        if (view == PonderDiagnosticView.SERVER || entryKey == null)
            return Collections.emptyList();
        List<PonderScene.ScheduledInstructionDiagnostic> timeline = javaTimelines.get(entryKey);
        return timeline == null ? Collections.<PonderScene.ScheduledInstructionDiagnostic>emptyList()
            : timeline;
    }

    static ClientViews mergeClientViews(List<PonderSceneDiagnostic> localScenes,
                                        List<PonderSceneDiagnostic> serverScenes) {
        List<PonderSceneDiagnostic> effectiveScenes = new ArrayList<PonderSceneDiagnostic>();
        Map<ResourceLocation, PonderSceneDiagnostic> serverById =
            new LinkedHashMap<ResourceLocation, PonderSceneDiagnostic>();
        for (PonderSceneDiagnostic scene : serverScenes)
            if (scene.getSceneId() != null)
                serverById.put(scene.getSceneId(), scene);
        List<PonderSceneDiagnostic> markedLocalScenes =
            new ArrayList<PonderSceneDiagnostic>(localScenes.size());
        for (PonderSceneDiagnostic scene : localScenes) {
            boolean script = scene.getSource() == PonderSceneSource.BUILTIN_ZS
                || scene.getSource() == PonderSceneSource.LOCAL_ZS;
            if (script && scene.getSceneId() != null && serverById.containsKey(scene.getSceneId())) {
                markedLocalScenes.add(scene.overriddenBy(PonderSceneSource.SERVER_SNAPSHOT)
                    .withIssue(issue("override.server_scene", PonderDiagnosticSeverity.INFO,
                        "Server snapshot overrides local script scene " + scene.getSceneId(),
                        scene.getSceneId(), -1)));
                continue;
            }
            markedLocalScenes.add(scene);
            effectiveScenes.add(scene);
        }
        effectiveScenes.addAll(serverScenes);
        return new ClientViews(markedLocalScenes, effectiveScenes);
    }

    private static PonderSceneDiagnostic buildJava(PonderSceneRegistry registry, StoryBoardEntry entry,
                                                   int index,
                                                   Map<String, List<PonderScene.ScheduledInstructionDiagnostic>>
                                                       timelines) {
        List<PonderDiagnosticIssue> issues = new ArrayList<PonderDiagnosticIssue>();
        ResourceLocation sceneId = entry.getDeclaredSceneId();
        String pluginClass = entry instanceof PonderStoryBoardEntry
            ? ((PonderStoryBoardEntry) entry).getPluginClass() : "";
        String pluginId = entry.getNamespace();
        PonderStructure structure = loadStructure(registry, entry.getSchematicLocation(), sceneId, issues);
        String title = "";
        int instructionCount = 0;
        int totalTicks = 0;
        List<Integer> keyframes = Collections.emptyList();
        String key = "java:" + pluginId + ":" + index + ":" + entry.getComponent();
        try {
            PonderScene compiled = registry.compileEntry(entry);
            ResourceLocation discovered = compiled.getId();
            title = compiled.getTitle();
            instructionCount = compiled.getScheduledInstructionCount();
            totalTicks = compiled.getTotalTicks();
            keyframes = compiled.getKeyframes();
            timelines.put(key, compiled.getScheduledInstructionDiagnostics());
            if (sceneId == null) {
                sceneId = discovered;
                issues.add(issue("registration.scene_id_undeclared", PonderDiagnosticSeverity.WARNING,
                    "Java storyboard did not declare its scene id with identifiedBy(...)", discovered, -1));
            } else if (!sceneId.equals(discovered)) {
                issues.add(issue("registration.scene_id_mismatch", PonderDiagnosticSeverity.ERROR,
                    "Declared scene id " + sceneId + " does not match compiled id " + discovered,
                    sceneId, -1));
            }
        } catch (RuntimeException failure) {
            issues.add(issue("compile.java_storyboard", PonderDiagnosticSeverity.ERROR,
                message(failure), sceneId, -1));
        }
        return scene(key, sceneId, entry.getComponent(), entry.getSchematicLocation(), title,
            PonderSceneSource.JAVA_PLUGIN, pluginClass, pluginId, structure, entry.getTags(),
            instructionCount, totalTicks, keyframes, issues);
    }

    private static PonderSceneDiagnostic buildScript(PonderSceneRegistry registry,
                                                     ScriptSceneDefinition definition,
                                                     PonderSceneSource source) {
        List<PonderDiagnosticIssue> issues = new ArrayList<PonderDiagnosticIssue>();
        PonderStructure structure = loadStructure(registry, definition.getStructure(),
            definition.getSceneId(), issues);
        int totalTicks = 0;
        List<Integer> keyframes = Collections.emptyList();
        try {
            PonderStoryBoardEntry entry = new PonderStoryBoardEntry(definition.asStoryBoard(),
                definition.getSceneId().getNamespace(), definition.getStructure(), definition.getComponent());
            entry.highlightTags(definition.getTags().toArray(new ResourceLocation[0]));
            entry.identifiedBy(definition.getSceneId());
            PonderScene compiled = registry.compileEntry(entry);
            totalTicks = compiled.getTotalTicks();
            keyframes = compiled.getKeyframes();
        } catch (RuntimeException failure) {
            issues.add(issue("ir.compile_failed", PonderDiagnosticSeverity.ERROR,
                message(failure), definition.getSceneId(), instructionIndex(failure)));
        }
        String sourceDescription = source == PonderSceneSource.SERVER_SNAPSHOT
            ? "server snapshot" : definition.getSourceDescription();
        return scene("script:" + source.name().toLowerCase(java.util.Locale.ROOT) + ":"
                + definition.getSceneId(), definition.getSceneId(), definition.getComponent(),
            definition.getStructure(), definition.getTitle(), source, sourceDescription,
            source == PonderSceneSource.SERVER_SNAPSHOT ? "server" : Ponder.CONTENT_NAMESPACE,
            structure, definition.getTags(), definition.getInstructions().size(), totalTicks,
            keyframes, issues);
    }

    private static PonderStructure loadStructure(PonderSceneRegistry registry, ResourceLocation structureId,
                                                 ResourceLocation sceneId,
                                                 List<PonderDiagnosticIssue> issues) {
        try {
            PonderStructure structure = registry.loadSchematic(structureId);
            for (String diagnostic : structure.getDiagnostics())
                issues.add(issue("structure.diagnostic", PonderDiagnosticSeverity.WARNING,
                    diagnostic, sceneId, -1));
            return structure;
        } catch (IOException failure) {
            issues.add(issue("structure.load_failed", PonderDiagnosticSeverity.ERROR,
                message(failure), sceneId, -1));
            return PonderStructure.missing(message(failure));
        } catch (RuntimeException failure) {
            issues.add(issue("structure.load_failed", PonderDiagnosticSeverity.ERROR,
                message(failure), sceneId, -1));
            return PonderStructure.missing(message(failure));
        }
    }

    private static PonderSceneDiagnostic scene(String key, ResourceLocation sceneId,
                                               ResourceLocation component, ResourceLocation structureId,
                                               String title, PonderSceneSource source,
                                               String sourceDescription, String pluginId,
                                               PonderStructure structure, List<ResourceLocation> tags,
                                               int instructionCount, int totalTicks, List<Integer> keyframes,
                                               List<PonderDiagnosticIssue> issues) {
        return new PonderSceneDiagnostic(key, sceneId, component, structureId, title, source,
            sourceDescription, pluginId, structure.getProviderId(), structure.getFingerprint(), tags,
            instructionCount, totalTicks, keyframes, issues, null);
    }

    private static List<PonderSceneDiagnostic> markDuplicateIds(List<PonderSceneDiagnostic> source) {
        Map<ResourceLocation, Integer> counts = new LinkedHashMap<ResourceLocation, Integer>();
        for (PonderSceneDiagnostic scene : source)
            if (scene.getSceneId() != null)
                counts.put(scene.getSceneId(), counts.containsKey(scene.getSceneId())
                    ? counts.get(scene.getSceneId()) + 1 : 1);
        List<PonderSceneDiagnostic> result = new ArrayList<PonderSceneDiagnostic>(source.size());
        for (PonderSceneDiagnostic scene : source) {
            if (scene.getSceneId() != null && counts.get(scene.getSceneId()) > 1)
                scene = scene.withIssue(issue("registration.duplicate_scene_id",
                    PonderDiagnosticSeverity.ERROR, "Duplicate scene id " + scene.getSceneId(),
                    scene.getSceneId(), -1));
            result.add(scene);
        }
        return result;
    }

    private static PonderDiagnosticSnapshot snapshot(PonderDiagnosticView view, long generation, long createdAt,
                                                     List<PonderSceneDiagnostic> scenes) {
        List<PonderDiagnosticIssue> issues = new ArrayList<PonderDiagnosticIssue>();
        for (PonderSceneDiagnostic scene : scenes)
            issues.addAll(scene.getIssues());
        return new PonderDiagnosticSnapshot(view, generation, createdAt, scenes, issues);
    }

    private static PonderDiagnosticSnapshot withIssue(PonderDiagnosticSnapshot snapshot,
                                                      PonderDiagnosticIssue issue) {
        List<PonderDiagnosticIssue> issues =
            new ArrayList<PonderDiagnosticIssue>(snapshot.getIssues());
        for (PonderDiagnosticIssue existing : issues)
            if (sameIssue(existing, issue))
                return snapshot;
        issues.add(issue);
        List<PonderSceneDiagnostic> scenes =
            new ArrayList<PonderSceneDiagnostic>(snapshot.getScenes().size());
        for (PonderSceneDiagnostic scene : snapshot.getScenes()) {
            if (issue.getSceneId() != null && issue.getSceneId().equals(scene.getSceneId()))
                scene = scene.withIssue(issue);
            scenes.add(scene);
        }
        return new PonderDiagnosticSnapshot(snapshot.getView(), snapshot.getGeneration(),
            snapshot.getCreatedAt(), scenes, issues);
    }

    private static boolean sameIssue(PonderDiagnosticIssue left, PonderDiagnosticIssue right) {
        return left.getSeverity() == right.getSeverity()
            && left.getCode().equals(right.getCode())
            && left.getMessage().equals(right.getMessage())
            && java.util.Objects.equals(left.getSceneId(), right.getSceneId())
            && left.getInstructionIndex() == right.getInstructionIndex();
    }

    private static PonderDiagnosticIssue issue(String code, PonderDiagnosticSeverity severity,
                                               String message, ResourceLocation sceneId,
                                               int instructionIndex) {
        return new PonderDiagnosticIssue(code, severity, message, sceneId, instructionIndex);
    }

    private static boolean isServerProcess() {
        try {
            net.minecraftforge.fml.relauncher.Side side = FMLCommonHandler.instance().getSide();
            return side != null && side.isServer();
        } catch (RuntimeException unavailableOutsideForgeLifecycle) {
            return false;
        }
    }

    private static ContributionResult contribute(PonderDiagnosticSnapshot base) {
        final List<PonderDiagnosticIssue> additions = new ArrayList<PonderDiagnosticIssue>();
        final List<PonderStructureDependency> dependencies = coreDependencies(base);
        for (final PonderDiagnosticContributor contributor : PonderDiagnosticContributors.snapshot()) {
            final ResourceLocation contributorId = contributor.getId();
            PonderDiagnosticSink sink = new PonderDiagnosticSink() {
                @Override
                public void reportIssue(String localCode, PonderDiagnosticSeverity severity,
                                        String message, ResourceLocation sceneId,
                                        int instructionIndex) {
                    additions.add(new PonderDiagnosticIssue(
                        contributorCode(contributorId, localCode), severity, message,
                        sceneId, instructionIndex));
                }

                @Override
                public void reportStructureDependency(ResourceLocation structureId,
                                                      ResourceLocation providerId,
                                                      String fingerprint,
                                                      PonderStructureDependencyStatus status,
                                                      java.util.Collection<ResourceLocation> sceneIds,
                                                      java.util.Collection<ResourceLocation> components) {
                    dependencies.add(new PonderStructureDependency(structureId, providerId,
                        fingerprint, status, sceneIds, components,
                        sourcesFor(base, sceneIds), contributorId));
                }
            };
            try {
                contributor.contribute(new PonderDiagnosticContext(base), sink);
            } catch (RuntimeException failure) {
                additions.add(issue("diagnostic.contributor_failed",
                    PonderDiagnosticSeverity.ERROR,
                    "Ponder diagnostic contributor " + contributorId + " failed: " + message(failure),
                    null, -1));
            }
        }
        PonderDiagnosticSnapshot result = base;
        for (PonderDiagnosticIssue addition : additions)
            result = withIssue(result, addition);
        return new ContributionResult(result, mergeDependencies(dependencies));
    }

    private static List<PonderStructureDependency> coreDependencies(
            PonderDiagnosticSnapshot snapshot) {
        List<PonderStructureDependency> result = new ArrayList<PonderStructureDependency>();
        for (PonderSceneDiagnostic scene : snapshot.getScenes()) {
            PonderStructureDependencyStatus status =
                net.createmod.ponder.api.structure.PonderStructureProviders.MISSING_ID.equals(
                    scene.getProviderId())
                    ? PonderStructureDependencyStatus.MISSING
                    : hasStructureError(scene)
                        ? PonderStructureDependencyStatus.ERROR
                        : PonderStructureDependencyStatus.AVAILABLE;
            result.add(new PonderStructureDependency(scene.getStructure(), scene.getProviderId(),
                scene.getFingerprint(), status,
                scene.getSceneId() == null
                    ? Collections.<ResourceLocation>emptyList()
                    : Collections.singletonList(scene.getSceneId()),
                Collections.singletonList(scene.getComponent()),
                Collections.singletonList(scene.getSource()), null));
        }
        return result;
    }

    private static boolean hasStructureError(PonderSceneDiagnostic scene) {
        for (PonderDiagnosticIssue diagnostic : scene.getIssues())
            if (diagnostic.getSeverity() == PonderDiagnosticSeverity.ERROR
                && diagnostic.getCode().startsWith("structure."))
                return true;
        return false;
    }

    private static List<PonderSceneSource> sourcesFor(PonderDiagnosticSnapshot snapshot,
                                                     java.util.Collection<ResourceLocation> sceneIds) {
        if (sceneIds == null || sceneIds.isEmpty())
            return Collections.emptyList();
        java.util.LinkedHashSet<PonderSceneSource> result =
            new java.util.LinkedHashSet<PonderSceneSource>();
        for (ResourceLocation sceneId : sceneIds) {
            PonderSceneDiagnostic scene = snapshot.findScene(sceneId);
            if (scene != null)
                result.add(scene.getSource());
        }
        return new ArrayList<PonderSceneSource>(result);
    }

    private static String contributorCode(ResourceLocation contributorId, String localCode) {
        if (localCode == null || !localCode.matches("[a-z0-9_.-]{1,48}"))
            throw new IllegalArgumentException("Invalid contributor diagnostic code: " + localCode);
        String owner = (contributorId.getNamespace() + "." + contributorId.getPath())
            .replaceAll("[^a-z0-9_.-]", ".");
        String result = "addon." + owner + "." + localCode;
        if (result.length() > 96)
            throw new IllegalArgumentException("Contributor diagnostic code is too long: " + result);
        return result;
    }

    private static List<PonderStructureDependency> mergeDependencies(
            List<PonderStructureDependency> source) {
        Map<String, MutableDependency> merged = new LinkedHashMap<String, MutableDependency>();
        for (PonderStructureDependency dependency : source) {
            String key = dependency.getStructureId() + "|"
                + String.valueOf(dependency.getProviderId()) + "|"
                + dependency.getFingerprint() + "|" + dependency.getStatus() + "|"
                + String.valueOf(dependency.getContributorId());
            MutableDependency value = merged.get(key);
            if (value == null) {
                value = new MutableDependency(dependency);
                merged.put(key, value);
            } else {
                value.sceneIds.addAll(dependency.getSceneIds());
                value.components.addAll(dependency.getComponents());
                value.sources.addAll(dependency.getSources());
            }
        }
        List<PonderStructureDependency> result =
            new ArrayList<PonderStructureDependency>(merged.size());
        for (MutableDependency value : merged.values())
            result.add(value.build());
        Collections.sort(result, new Comparator<PonderStructureDependency>() {
            @Override
            public int compare(PonderStructureDependency left,
                               PonderStructureDependency right) {
                int structure = left.getStructureId().toString()
                    .compareTo(right.getStructureId().toString());
                if (structure != 0)
                    return structure;
                return String.valueOf(left.getContributorId())
                    .compareTo(String.valueOf(right.getContributorId()));
            }
        });
        return Collections.unmodifiableList(result);
    }

    private static Map<String, List<PonderScene.ScheduledInstructionDiagnostic>> immutableTimelines(
            Map<String, List<PonderScene.ScheduledInstructionDiagnostic>> source) {
        if (source.isEmpty())
            return Collections.emptyMap();
        Map<String, List<PonderScene.ScheduledInstructionDiagnostic>> copy =
            new LinkedHashMap<String, List<PonderScene.ScheduledInstructionDiagnostic>>();
        for (Map.Entry<String, List<PonderScene.ScheduledInstructionDiagnostic>> entry : source.entrySet())
            copy.put(entry.getKey(), Collections.unmodifiableList(
                new ArrayList<PonderScene.ScheduledInstructionDiagnostic>(entry.getValue())));
        return Collections.unmodifiableMap(copy);
    }

    private static int instructionIndex(Throwable failure) {
        String message = failure.getMessage();
        if (message == null)
            return -1;
        java.util.regex.Matcher matcher = java.util.regex.Pattern
            .compile("instruction\\s+(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(message);
        if (!matcher.find())
            return -1;
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static String message(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.trim().isEmpty()
            ? failure.getClass().getSimpleName() : message;
    }

    static final class ClientViews {
        final List<PonderSceneDiagnostic> local;
        final List<PonderSceneDiagnostic> effective;

        ClientViews(List<PonderSceneDiagnostic> local, List<PonderSceneDiagnostic> effective) {
            this.local = local;
            this.effective = effective;
        }
    }

    private static final class ContributionResult {
        final PonderDiagnosticSnapshot snapshot;
        final List<PonderStructureDependency> dependencies;

        ContributionResult(PonderDiagnosticSnapshot snapshot,
                           List<PonderStructureDependency> dependencies) {
            this.snapshot = snapshot;
            this.dependencies = dependencies;
        }
    }

    private static final class MutableDependency {
        final ResourceLocation structureId;
        final ResourceLocation providerId;
        final String fingerprint;
        final PonderStructureDependencyStatus status;
        final ResourceLocation contributorId;
        final java.util.LinkedHashSet<ResourceLocation> sceneIds =
            new java.util.LinkedHashSet<ResourceLocation>();
        final java.util.LinkedHashSet<ResourceLocation> components =
            new java.util.LinkedHashSet<ResourceLocation>();
        final java.util.LinkedHashSet<PonderSceneSource> sources =
            new java.util.LinkedHashSet<PonderSceneSource>();

        MutableDependency(PonderStructureDependency source) {
            structureId = source.getStructureId();
            providerId = source.getProviderId();
            fingerprint = source.getFingerprint();
            status = source.getStatus();
            contributorId = source.getContributorId();
            sceneIds.addAll(source.getSceneIds());
            components.addAll(source.getComponents());
            sources.addAll(source.getSources());
        }

        PonderStructureDependency build() {
            return new PonderStructureDependency(structureId, providerId, fingerprint, status,
                sceneIds, components, sources, contributorId);
        }
    }
}
