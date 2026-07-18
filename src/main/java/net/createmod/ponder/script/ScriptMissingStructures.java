package net.createmod.ponder.script;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.createmod.ponder.foundation.structure.PonderStructureLoader;
import net.minecraft.util.ResourceLocation;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticIssue;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticSeverity;

public final class ScriptMissingStructures {
    private static final Set<String> PENDING = new LinkedHashSet<String>();
    private static final List<PonderDiagnosticIssue> PENDING_ISSUES =
        new ArrayList<PonderDiagnosticIssue>();

    private ScriptMissingStructures() {
    }

    public static synchronized void record(ResourceLocation sceneId, ResourceLocation structureId) {
        String message = "Skipped Ponder scene " + sceneId + ": missing structure " + structureId
            + " (expected " + PonderStructureLoader.expectedExternalPath(structureId) + ")";
        PENDING.add(message);
        PonderDiagnosticIssue issue = new PonderDiagnosticIssue("structure.missing",
            PonderDiagnosticSeverity.ERROR, message, sceneId, -1);
        boolean duplicate = false;
        for (PonderDiagnosticIssue pending : PENDING_ISSUES)
            if (pending.getCode().equals(issue.getCode())
                && pending.getMessage().equals(issue.getMessage())
                && java.util.Objects.equals(pending.getSceneId(), issue.getSceneId())) {
                duplicate = true;
                break;
            }
        if (!duplicate)
            PENDING_ISSUES.add(issue);
    }

    public static synchronized List<String> drain() {
        List<String> result = new ArrayList<String>(PENDING);
        PENDING.clear();
        return result;
    }

    public static synchronized List<PonderDiagnosticIssue> drainDiagnosticIssues() {
        List<PonderDiagnosticIssue> result =
            new ArrayList<PonderDiagnosticIssue>(PENDING_ISSUES);
        PENDING_ISSUES.clear();
        return result;
    }
}
