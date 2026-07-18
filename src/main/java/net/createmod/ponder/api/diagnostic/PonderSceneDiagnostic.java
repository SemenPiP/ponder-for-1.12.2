package net.createmod.ponder.api.diagnostic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.util.ResourceLocation;

public final class PonderSceneDiagnostic {
    private final String entryKey;
    private final ResourceLocation sceneId;
    private final ResourceLocation component;
    private final ResourceLocation structure;
    private final String title;
    private final PonderSceneSource source;
    private final String sourceDescription;
    private final String pluginId;
    private final ResourceLocation providerId;
    private final String fingerprint;
    private final List<ResourceLocation> tags;
    private final int instructionCount;
    private final int totalTicks;
    private final List<Integer> keyframes;
    private final List<PonderDiagnosticIssue> issues;
    private final PonderSceneSource overriddenBy;

    public PonderSceneDiagnostic(String entryKey, ResourceLocation sceneId, ResourceLocation component,
                                 ResourceLocation structure, String title, PonderSceneSource source,
                                 String sourceDescription, String pluginId, ResourceLocation providerId,
                                 String fingerprint, List<ResourceLocation> tags, int instructionCount,
                                 int totalTicks, List<Integer> keyframes, List<PonderDiagnosticIssue> issues,
                                 PonderSceneSource overriddenBy) {
        if (entryKey == null || entryKey.trim().isEmpty())
            throw new IllegalArgumentException("Ponder diagnostic entry key is required");
        if (component == null || structure == null || source == null)
            throw new IllegalArgumentException("Ponder diagnostic identity is required");
        this.entryKey = entryKey;
        this.sceneId = sceneId;
        this.component = component;
        this.structure = structure;
        this.title = title == null ? "" : title;
        this.source = source;
        this.sourceDescription = sourceDescription == null ? "" : sourceDescription;
        this.pluginId = pluginId == null ? "" : pluginId;
        this.providerId = providerId;
        this.fingerprint = fingerprint == null ? "" : fingerprint;
        this.tags = immutable(tags);
        this.instructionCount = Math.max(0, instructionCount);
        this.totalTicks = Math.max(0, totalTicks);
        this.keyframes = immutableIntegers(keyframes);
        this.issues = immutableIssues(issues);
        this.overriddenBy = overriddenBy;
    }

    public String getEntryKey() { return entryKey; }
    public ResourceLocation getSceneId() { return sceneId; }
    public ResourceLocation getComponent() { return component; }
    public ResourceLocation getStructure() { return structure; }
    public String getTitle() { return title; }
    public PonderSceneSource getSource() { return source; }
    public String getSourceDescription() { return sourceDescription; }
    public String getPluginId() { return pluginId; }
    public ResourceLocation getProviderId() { return providerId; }
    public String getFingerprint() { return fingerprint; }
    public List<ResourceLocation> getTags() { return tags; }
    public int getInstructionCount() { return instructionCount; }
    public int getTotalTicks() { return totalTicks; }
    public List<Integer> getKeyframes() { return keyframes; }
    public List<PonderDiagnosticIssue> getIssues() { return issues; }
    public PonderSceneSource getOverriddenBy() { return overriddenBy; }
    public boolean isOverridden() { return overriddenBy != null; }

    public boolean hasErrors() {
        for (PonderDiagnosticIssue issue : issues)
            if (issue.getSeverity() == PonderDiagnosticSeverity.ERROR)
                return true;
        return false;
    }

    public PonderSceneDiagnostic overriddenBy(PonderSceneSource replacement) {
        return new PonderSceneDiagnostic(entryKey, sceneId, component, structure, title, source,
            sourceDescription, pluginId, providerId, fingerprint, tags, instructionCount, totalTicks,
            keyframes, issues, replacement);
    }

    public PonderSceneDiagnostic withIssue(PonderDiagnosticIssue issue) {
        List<PonderDiagnosticIssue> replacement = new ArrayList<PonderDiagnosticIssue>(issues);
        replacement.add(issue);
        return new PonderSceneDiagnostic(entryKey, sceneId, component, structure, title, source,
            sourceDescription, pluginId, providerId, fingerprint, tags, instructionCount, totalTicks,
            keyframes, replacement, overriddenBy);
    }

    private static List<ResourceLocation> immutable(List<ResourceLocation> source) {
        if (source == null || source.isEmpty())
            return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<ResourceLocation>(source));
    }

    private static List<Integer> immutableIntegers(List<Integer> source) {
        if (source == null || source.isEmpty())
            return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<Integer>(source));
    }

    private static List<PonderDiagnosticIssue> immutableIssues(List<PonderDiagnosticIssue> source) {
        if (source == null || source.isEmpty())
            return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<PonderDiagnosticIssue>(source));
    }
}
