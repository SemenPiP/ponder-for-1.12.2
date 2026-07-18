package net.createmod.ponder.foundation.diagnostic;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import net.createmod.ponder.api.diagnostic.PonderDiagnosticIssue;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticSnapshot;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticView;
import net.createmod.ponder.api.diagnostic.PonderSceneDiagnostic;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.script.ScriptSceneDefinition;
import net.createmod.ponder.script.ScriptSceneRegistry;
import net.minecraft.util.ResourceLocation;

public final class PonderDiagnosticService {
    public static final int PAGE_SIZE = 10;

    private PonderDiagnosticService() {
    }

    public static void execute(String side, String request, Consumer<String> output) {
        String[] arguments = request == null || request.trim().isEmpty()
            ? new String[0] : request.trim().split("\\s+");
        if (arguments.length == 0)
            throw new IllegalArgumentException("Missing Ponder diagnostic command");
        String command = arguments[0].toLowerCase(Locale.ROOT);
        if ("list".equals(command)) {
            PonderDiagnosticView view = view(arguments, 1);
            int page = arguments.length > 2 ? positive(arguments[2], "page") : 1;
            list(PonderIndex.getDiagnosticSnapshot(view), page, output);
            return;
        }
        if ("inspect".equals(command)) {
            require(arguments, 2, "inspect requires a scene id");
            PonderDiagnosticView view = view(arguments, 2);
            inspect(PonderIndex.getDiagnosticSnapshot(view), new ResourceLocation(arguments[1]), output);
            return;
        }
        if ("validate".equals(command)) {
            PonderDiagnosticView view = view(arguments, 1);
            if (!PonderValidationManager.start(side, PonderIndex.getDiagnosticSnapshot(view), output))
                output.accept("A Ponder validation task is already running on this side.");
            return;
        }
        if ("export".equals(command)) {
            require(arguments, 2, "export requires a scene id");
            String mode = arguments.length > 2 ? arguments[2].toLowerCase(Locale.ROOT) : "all";
            PonderDiagnosticView view = view(arguments, 3);
            export(PonderIndex.getDiagnosticSnapshot(view), new ResourceLocation(arguments[1]),
                mode, view, output);
            return;
        }
        throw new IllegalArgumentException("Unknown Ponder diagnostic command: " + command);
    }

    public static List<String> sceneIds(PonderDiagnosticView view) {
        List<String> result = new ArrayList<String>();
        for (PonderSceneDiagnostic scene : PonderIndex.getDiagnosticSnapshot(view).getScenes())
            if (scene.getSceneId() != null)
                result.add(scene.getSceneId().toString());
        return Collections.unmodifiableList(result);
    }

    private static void list(PonderDiagnosticSnapshot snapshot, int page, Consumer<String> output) {
        int pages = Math.max(1, (snapshot.getScenes().size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page > pages)
            throw new IllegalArgumentException("Ponder diagnostic page must be 1.." + pages);
        output.accept("Ponder " + snapshot.getView().name().toLowerCase(Locale.ROOT) + " scenes, page "
            + page + "/" + pages + " (" + snapshot.getScenes().size() + " total)");
        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(snapshot.getScenes().size(), start + PAGE_SIZE);
        for (int i = start; i < end; i++) {
            PonderSceneDiagnostic scene = snapshot.getScenes().get(i);
            output.accept((scene.hasErrors() ? "[ERROR] " : "[OK] ")
                + (scene.getSceneId() == null ? scene.getEntryKey() : scene.getSceneId())
                + " | " + scene.getSource() + " | " + scene.getComponent()
                + " | " + scene.getStructure());
        }
    }

    private static void inspect(PonderDiagnosticSnapshot snapshot, ResourceLocation sceneId,
                                Consumer<String> output) {
        PonderSceneDiagnostic scene = snapshot.findScene(sceneId);
        if (scene == null)
            throw new IllegalArgumentException("Unknown Ponder scene " + sceneId + " in "
                + snapshot.getView().name().toLowerCase(Locale.ROOT));
        output.accept("Ponder scene " + sceneId + " [" + scene.getSource() + "]");
        output.accept("Component: " + scene.getComponent() + "; structure: " + scene.getStructure());
        output.accept("Source: " + (scene.getSourceDescription().isEmpty()
            ? scene.getPluginId() : scene.getSourceDescription()));
        output.accept("Provider: " + scene.getProviderId() + "; fingerprint: " + scene.getFingerprint());
        if (scene.isOverridden())
            output.accept("Overridden by: " + scene.getOverriddenBy());
        output.accept("Instructions: " + scene.getInstructionCount() + "; ticks: " + scene.getTotalTicks()
            + "; keyframes: " + scene.getKeyframes());
        for (PonderDiagnosticIssue issue : scene.getIssues())
            output.accept(issue.getSeverity() + " " + issue.getCode() + ": " + issue.getMessage());
    }

    private static void export(PonderDiagnosticSnapshot snapshot, ResourceLocation sceneId, String mode,
                               PonderDiagnosticView view, Consumer<String> output) {
        if (!"ir".equals(mode) && !"timeline".equals(mode) && !"all".equals(mode))
            throw new IllegalArgumentException("Export mode must be ir, timeline or all");
        PonderSceneDiagnostic scene = snapshot.findScene(sceneId);
        if (scene == null)
            throw new IllegalArgumentException("Unknown Ponder scene " + sceneId);
        ScriptSceneDefinition definition = ScriptSceneRegistry.find(view, sceneId);
        try {
            if ("ir".equals(mode)) {
                if (definition == null)
                    throw new IllegalArgumentException("Java Ponder scenes do not have exportable script IR");
                File ir = PonderDiagnosticReports.writeIr(scene, definition);
                output.accept("Ponder IR exported to " + ir.getPath());
            }
            if ("all".equals(mode)) {
                if (definition == null) {
                    output.accept("Ponder IR export skipped: Java scenes do not have script IR.");
                } else {
                    File ir = PonderDiagnosticReports.writeIr(scene, definition);
                    output.accept("Ponder IR exported to " + ir.getPath());
                }
            }
            if ("timeline".equals(mode) || "all".equals(mode)) {
                File timeline = PonderDiagnosticReports.writeTimeline(scene, definition,
                    PonderDiagnosticRegistry.javaTimeline(view, scene.getEntryKey()));
                output.accept("Ponder timeline exported to " + timeline.getPath());
            }
        } catch (Exception failure) {
            throw new IllegalArgumentException("Could not export Ponder scene " + sceneId + ": "
                + failure.getMessage(), failure);
        }
    }

    private static PonderDiagnosticView view(String[] arguments, int index) {
        return arguments.length > index ? PonderDiagnosticView.parse(arguments[index])
            : PonderDiagnosticView.EFFECTIVE;
    }

    private static int positive(String value, String label) {
        try {
            int number = Integer.parseInt(value);
            if (number < 1)
                throw new NumberFormatException();
            return number;
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(label + " must be a positive integer");
        }
    }

    private static void require(String[] arguments, int count, String message) {
        if (arguments.length < count)
            throw new IllegalArgumentException(message);
    }
}
