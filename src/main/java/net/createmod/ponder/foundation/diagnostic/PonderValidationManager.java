package net.createmod.ponder.foundation.diagnostic;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

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
        TASKS.put(side, new Task(snapshot, output));
        output.accept("Ponder validation started for " + snapshot.getScenes().size() + " scene(s).");
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
            if (scene.hasErrors())
                task.errors++;
            task.warnings += scene.getIssues().size();
            processed++;
        }
        if (task.index < task.snapshot.getScenes().size())
            return;
        try {
            File report = PonderDiagnosticReports.writeValidation(task.snapshot);
            task.output.accept("Ponder validation complete: " + task.errors + " scene error(s), "
                + task.warnings + " issue(s). Report: " + report.getPath());
        } catch (IOException failure) {
            task.output.accept("Ponder validation completed but the report could not be written: "
                + failure.getMessage());
        }
        TASKS.remove(side);
    }

    public static synchronized boolean isRunning(String side) {
        return TASKS.containsKey(side);
    }

    private static final class Task {
        final PonderDiagnosticSnapshot snapshot;
        final Consumer<String> output;
        int index;
        int errors;
        int warnings;

        Task(PonderDiagnosticSnapshot snapshot, Consumer<String> output) {
            this.snapshot = snapshot;
            this.output = output;
        }
    }
}
