package net.createmod.ponder.api.diagnostic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.util.ResourceLocation;

public final class PonderDiagnosticSnapshot {
    private final PonderDiagnosticView view;
    private final long generation;
    private final long createdAt;
    private final List<PonderSceneDiagnostic> scenes;
    private final List<PonderDiagnosticIssue> issues;

    public PonderDiagnosticSnapshot(PonderDiagnosticView view, long generation, long createdAt,
                                    List<PonderSceneDiagnostic> scenes,
                                    List<PonderDiagnosticIssue> issues) {
        if (view == null)
            throw new IllegalArgumentException("Ponder diagnostic view is required");
        this.view = view;
        this.generation = generation;
        this.createdAt = createdAt;
        this.scenes = immutableScenes(scenes);
        this.issues = immutableIssues(issues);
    }

    public static PonderDiagnosticSnapshot empty(PonderDiagnosticView view) {
        return new PonderDiagnosticSnapshot(view, 0, System.currentTimeMillis(),
            Collections.<PonderSceneDiagnostic>emptyList(),
            Collections.<PonderDiagnosticIssue>emptyList());
    }

    public PonderDiagnosticView getView() { return view; }
    public long getGeneration() { return generation; }
    public long getCreatedAt() { return createdAt; }
    public List<PonderSceneDiagnostic> getScenes() { return scenes; }
    public List<PonderDiagnosticIssue> getIssues() { return issues; }

    public PonderSceneDiagnostic findScene(ResourceLocation sceneId) {
        if (sceneId == null)
            return null;
        for (PonderSceneDiagnostic scene : scenes)
            if (sceneId.equals(scene.getSceneId()))
                return scene;
        return null;
    }

    private static List<PonderSceneDiagnostic> immutableScenes(List<PonderSceneDiagnostic> source) {
        if (source == null || source.isEmpty())
            return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<PonderSceneDiagnostic>(source));
    }

    private static List<PonderDiagnosticIssue> immutableIssues(List<PonderDiagnosticIssue> source) {
        if (source == null || source.isEmpty())
            return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<PonderDiagnosticIssue>(source));
    }
}
