package net.createmod.ponder.foundation.diagnostic;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import net.createmod.ponder.api.diagnostic.PonderDiagnosticIssue;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticSeverity;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticSnapshot;
import net.createmod.ponder.api.diagnostic.PonderSceneDiagnostic;

public final class PonderValidationManager {
    private static final Map<String, Task> TASKS = new LinkedHashMap<String, Task>();

    private PonderValidationManager() {
    }

    public static synchronized boolean start(String side, PonderDiagnosticSnapshot snapshot,
                                             Consumer<String> output) {
        if (TASKS.containsKey(side))
            return false;
        TASKS.put(side, new Task(side, snapshot, output));
        output.accept(PonderDiagnosticService.text(side, "ponder.validation.started",
            snapshot.getScenes().size()));
        return true;
    }

    public static synchronized void tick(String side) {
        Task task = TASKS.get(side);
        if (task == null)
            return;
        long deadline = System.nanoTime() + 8_000_000L;
        int processed = 0;
        while (task.index < task.snapshot.getScenes().size() && processed < 1
            && System.nanoTime() <= deadline) {
            PonderSceneDiagnostic scene = task.snapshot.getScenes().get(task.index++);
            for (PonderDiagnosticIssue issue : scene.getIssues()) {
                task.count(issue);
                task.countedIssues.add(issue);
            }
            processed++;
        }
        if (task.index < task.snapshot.getScenes().size())
            return;
        for (PonderDiagnosticIssue issue : task.snapshot.getIssues())
            if (!task.countedIssues.contains(issue))
                task.count(issue);
        try {
            File report = PonderDiagnosticReports.writeValidation(task.snapshot);
            task.output.accept(PonderDiagnosticService.text(task.side, "ponder.validation.complete",
                task.errors, task.warnings, report.getPath()));
        } catch (IOException failure) {
            task.output.accept(PonderDiagnosticService.text(task.side, "ponder.validation.report_failed",
                failure.getMessage()));
        }
        TASKS.remove(side);
    }

    public static synchronized boolean isRunning(String side) {
        return TASKS.containsKey(side);
    }

    private static final class Task {
        final String side;
        final PonderDiagnosticSnapshot snapshot;
        final Consumer<String> output;
        final Set<PonderDiagnosticIssue> countedIssues =
            Collections.newSetFromMap(new IdentityHashMap<PonderDiagnosticIssue, Boolean>());
        int index;
        int errors;
        int warnings;

        Task(String side, PonderDiagnosticSnapshot snapshot, Consumer<String> output) {
            this.side = side;
            this.snapshot = snapshot;
            this.output = output;
        }

        void count(PonderDiagnosticIssue issue) {
            if (issue.getSeverity() == PonderDiagnosticSeverity.ERROR)
                errors++;
            else if (issue.getSeverity() == PonderDiagnosticSeverity.WARNING)
                warnings++;
        }
    }
}
