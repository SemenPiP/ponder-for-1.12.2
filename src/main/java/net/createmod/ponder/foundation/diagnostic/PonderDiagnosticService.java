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
import net.createmod.ponder.api.diagnostic.PonderStructureDependency;
import net.createmod.ponder.api.diagnostic.PonderStructureDependencyStatus;
import net.createmod.ponder.api.script.ScriptInstructionCodecDescriptor;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.script.ScriptSceneDefinition;
import net.createmod.ponder.script.ScriptSceneRegistry;
import net.createmod.ponder.script.ScriptSceneSnapshot;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;

public final class PonderDiagnosticService {
    public static final int PAGE_SIZE = 10;

    private PonderDiagnosticService() {
    }

    public static void execute(String side, String request, Consumer<String> output) {
        String[] arguments = request == null || request.trim().isEmpty()
            ? new String[0] : request.trim().split("\\s+");
        if (arguments.length == 0)
            throw new IllegalArgumentException(text(side, "ponder.diagnostic.missing_command"));
        String command = arguments[0].toLowerCase(Locale.ROOT);
        if ("list".equals(command)) {
            PonderDiagnosticView view = view(side, arguments, 1);
            int page = arguments.length > 2 ? positive(side, arguments[2]) : 1;
            list(side, PonderIndex.getDiagnosticSnapshot(view), page, output);
            return;
        }
        if ("inspect".equals(command)) {
            require(side, arguments, 2, "ponder.diagnostic.inspect_requires_scene");
            PonderDiagnosticView view = view(side, arguments, 2);
            inspect(side, PonderIndex.getDiagnosticSnapshot(view),
                new ResourceLocation(arguments[1]), output);
            return;
        }
        if ("validate".equals(command)) {
            PonderDiagnosticView view = view(side, arguments, 1);
            if (!PonderValidationManager.start(side, PonderIndex.getDiagnosticSnapshot(view), output))
                output.accept(text(side, "ponder.validation.already_running"));
            return;
        }
        if ("export".equals(command)) {
            require(side, arguments, 2, "ponder.diagnostic.export_requires_scene");
            String mode = arguments.length > 2 ? arguments[2].toLowerCase(Locale.ROOT) : "all";
            PonderDiagnosticView view = view(side, arguments, 3);
            export(side, PonderIndex.getDiagnosticSnapshot(view),
                new ResourceLocation(arguments[1]), mode, view, output);
            return;
        }
        if ("dependencies".equals(command)) {
            PonderDiagnosticView view = view(side, arguments, 1);
            dependencies(side, view, output);
            return;
        }
        throw new IllegalArgumentException(text(side, "ponder.diagnostic.unknown_command", command));
    }

    public static List<String> sceneIds(PonderDiagnosticView view) {
        List<String> result = new ArrayList<String>();
        for (PonderSceneDiagnostic scene : PonderIndex.getDiagnosticSnapshot(view).getScenes())
            if (scene.getSceneId() != null)
                result.add(scene.getSceneId().toString());
        return Collections.unmodifiableList(result);
    }

    private static void list(String side, PonderDiagnosticSnapshot snapshot, int page,
                             Consumer<String> output) {
        int pages = Math.max(1, (snapshot.getScenes().size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page > pages)
            throw new IllegalArgumentException(text(side, "ponder.diagnostic.page_out_of_range", pages));
        output.accept(text(side, "ponder.diagnostic.list_header",
            snapshot.getView().name().toLowerCase(Locale.ROOT), page, pages, snapshot.getScenes().size()));
        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(snapshot.getScenes().size(), start + PAGE_SIZE);
        for (int i = start; i < end; i++) {
            PonderSceneDiagnostic scene = snapshot.getScenes().get(i);
            output.accept(text(side, "ponder.diagnostic.list_entry",
                text(side, scene.hasErrors() ? "ponder.diagnostic.error_marker"
                    : "ponder.diagnostic.ok_marker"),
                scene.getSceneId() == null ? scene.getEntryKey() : scene.getSceneId(),
                scene.getSource(), scene.getComponent(), scene.getStructure()));
        }
    }

    private static void inspect(String side, PonderDiagnosticSnapshot snapshot, ResourceLocation sceneId,
                                Consumer<String> output) {
        PonderSceneDiagnostic scene = snapshot.findScene(sceneId);
        if (scene == null)
            throw new IllegalArgumentException(text(side, "ponder.diagnostic.unknown_scene",
                sceneId, snapshot.getView().name().toLowerCase(Locale.ROOT)));
        output.accept(text(side, "ponder.diagnostic.scene_header", sceneId, scene.getSource()));
        output.accept(text(side, "ponder.diagnostic.component",
            scene.getComponent(), scene.getStructure()));
        output.accept(text(side, "ponder.diagnostic.source", scene.getSourceDescription().isEmpty()
            ? scene.getPluginId() : scene.getSourceDescription()));
        output.accept(text(side, "ponder.diagnostic.provider",
            scene.getProviderId(), scene.getFingerprint()));
        if (scene.isOverridden())
            output.accept(text(side, "ponder.diagnostic.overridden_by", scene.getOverriddenBy()));
        output.accept(text(side, "ponder.diagnostic.instructions", scene.getInstructionCount(),
            scene.getTotalTicks(), scene.getKeyframes()));
        output.accept(text(side, "ponder.diagnostic.tags", scene.getTags()));
        ScriptSceneDefinition definition = ScriptSceneRegistry.find(snapshot.getView(), sceneId);
        if (definition != null) {
            try {
                List<ScriptInstructionCodecDescriptor> codecs =
                    ScriptSceneSnapshot.requiredCodecs(Collections.singletonList(definition));
                if (!codecs.isEmpty())
                    output.accept(text(side, "ponder.diagnostic.codecs", codecs));
            } catch (java.io.IOException failure) {
                output.accept(text(side, "ponder.diagnostic.codecs_failed", failure.getMessage()));
            }
        }
        for (PonderDiagnosticIssue issue : scene.getIssues())
            output.accept(text(side, "ponder.diagnostic.issue",
                text(side, "ponder.diagnostic.severity." + issue.getSeverity().name().toLowerCase(Locale.ROOT)),
                issue.getCode(), issue.getMessage()));
    }

    private static void dependencies(String side, PonderDiagnosticView view,
                                     Consumer<String> output) {
        List<PonderStructureDependency> dependencies =
            PonderIndex.getStructureDependencies(view);
        try {
            File report = PonderDiagnosticReports.writeDependencies(view, dependencies);
            int missing = 0;
            int errors = 0;
            for (PonderStructureDependency dependency : dependencies) {
                if (dependency.getStatus() == PonderStructureDependencyStatus.MISSING)
                    missing++;
                else if (dependency.getStatus() == PonderStructureDependencyStatus.ERROR)
                    errors++;
            }
            output.accept(text(side, "ponder.diagnostic.dependencies_exported",
                dependencies.size(), missing, errors, report.getPath()));
        } catch (java.io.IOException failure) {
            throw new IllegalArgumentException(text(side,
                "ponder.diagnostic.dependencies_failed", failure.getMessage()), failure);
        }
    }

    private static void export(String side, PonderDiagnosticSnapshot snapshot, ResourceLocation sceneId,
                               String mode,
                               PonderDiagnosticView view, Consumer<String> output) {
        if (!"ir".equals(mode) && !"timeline".equals(mode) && !"all".equals(mode))
            throw new IllegalArgumentException(text(side, "ponder.diagnostic.invalid_export_mode"));
        PonderSceneDiagnostic scene = snapshot.findScene(sceneId);
        if (scene == null)
            throw new IllegalArgumentException(text(side, "ponder.diagnostic.unknown_scene_simple", sceneId));
        ScriptSceneDefinition definition = ScriptSceneRegistry.find(view, sceneId);
        try {
            if ("ir".equals(mode)) {
                if (definition == null)
                    throw new IllegalArgumentException(
                        text(side, "ponder.diagnostic.java_scene_no_ir"));
                File ir = PonderDiagnosticReports.writeIr(scene, definition);
                output.accept(text(side, "ponder.diagnostic.ir_exported", ir.getPath()));
            }
            if ("all".equals(mode)) {
                if (definition == null) {
                    output.accept(text(side, "ponder.diagnostic.ir_export_skipped"));
                } else {
                    File ir = PonderDiagnosticReports.writeIr(scene, definition);
                    output.accept(text(side, "ponder.diagnostic.ir_exported", ir.getPath()));
                }
            }
            if ("timeline".equals(mode) || "all".equals(mode)) {
                File timeline = PonderDiagnosticReports.writeTimeline(scene, definition,
                    PonderDiagnosticRegistry.javaTimeline(view, scene.getEntryKey()));
                output.accept(text(side, "ponder.diagnostic.timeline_exported", timeline.getPath()));
            }
        } catch (Exception failure) {
            throw new IllegalArgumentException(text(side, "ponder.diagnostic.export_failed",
                sceneId, failure.getMessage()), failure);
        }
    }

    private static PonderDiagnosticView view(String side, String[] arguments, int index) {
        if (arguments.length <= index)
            return PonderDiagnosticView.EFFECTIVE;
        try {
            return PonderDiagnosticView.parse(arguments[index]);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException(text(side, "ponder.diagnostic.invalid_view",
                arguments[index]));
        }
    }

    private static int positive(String side, String value) {
        try {
            int number = Integer.parseInt(value);
            if (number < 1)
                throw new NumberFormatException();
            return number;
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(text(side, "ponder.diagnostic.page_positive"));
        }
    }

    private static void require(String side, String[] arguments, int count, String key) {
        if (arguments.length < count)
            throw new IllegalArgumentException(text(side, key));
    }

    static String text(String side, String key, Object... arguments) {
        String english = english(key, arguments);
        if (!"client".equalsIgnoreCase(side))
            return english;
        try {
            String translated = new TextComponentTranslation(key, arguments).getUnformattedText();
            return translated == null || translated.equals(key) ? english : translated;
        } catch (RuntimeException ignored) {
            return english;
        }
    }

    private static String english(String key, Object[] arguments) {
        String format;
        if ("ponder.diagnostic.missing_command".equals(key))
            format = "Missing Ponder diagnostic command";
        else if ("ponder.diagnostic.unknown_command".equals(key))
            format = "Unknown Ponder diagnostic command: %1$s";
        else if ("ponder.diagnostic.invalid_view".equals(key))
            format = "Unknown Ponder diagnostic view: %1$s";
        else if ("ponder.diagnostic.inspect_requires_scene".equals(key))
            format = "Ponder inspect requires a scene id";
        else if ("ponder.diagnostic.export_requires_scene".equals(key))
            format = "Ponder export requires a scene id";
        else if ("ponder.validation.already_running".equals(key))
            format = "A Ponder validation task is already running on this side.";
        else if ("ponder.validation.started".equals(key))
            format = "Ponder validation started for %1$s scene(s).";
        else if ("ponder.diagnostic.page_out_of_range".equals(key))
            format = "Ponder diagnostic page must be 1..%1$s";
        else if ("ponder.diagnostic.page_positive".equals(key))
            format = "page must be a positive integer";
        else if ("ponder.diagnostic.list_header".equals(key))
            format = "Ponder %1$s scenes, page %2$s/%3$s (%4$s total)";
        else if ("ponder.diagnostic.list_entry".equals(key))
            format = "%1$s %2$s | %3$s | %4$s | %5$s";
        else if ("ponder.diagnostic.error_marker".equals(key))
            format = "[ERROR]";
        else if ("ponder.diagnostic.ok_marker".equals(key))
            format = "[OK]";
        else if ("ponder.diagnostic.unknown_scene".equals(key))
            format = "Unknown Ponder scene %1$s in %2$s";
        else if ("ponder.diagnostic.scene_header".equals(key))
            format = "Ponder scene %1$s [%2$s]";
        else if ("ponder.diagnostic.component".equals(key))
            format = "Component: %1$s; structure: %2$s";
        else if ("ponder.diagnostic.source".equals(key))
            format = "Source: %1$s";
        else if ("ponder.diagnostic.provider".equals(key))
            format = "Provider: %1$s; fingerprint: %2$s";
        else if ("ponder.diagnostic.overridden_by".equals(key))
            format = "Overridden by: %1$s";
        else if ("ponder.diagnostic.instructions".equals(key))
            format = "Instructions: %1$s; ticks: %2$s; keyframes: %3$s";
        else if ("ponder.diagnostic.tags".equals(key))
            format = "Tags: %1$s";
        else if ("ponder.diagnostic.codecs".equals(key))
            format = "Required codecs: %1$s";
        else if ("ponder.diagnostic.codecs_failed".equals(key))
            format = "Could not inspect required codecs: %1$s";
        else if ("ponder.diagnostic.issue".equals(key))
            format = "%1$s %2$s: %3$s";
        else if (key.startsWith("ponder.diagnostic.severity."))
            format = key.substring("ponder.diagnostic.severity.".length()).toUpperCase(Locale.ROOT);
        else if ("ponder.diagnostic.invalid_export_mode".equals(key))
            format = "Export mode must be ir, timeline or all";
        else if ("ponder.diagnostic.unknown_scene_simple".equals(key))
            format = "Unknown Ponder scene %1$s";
        else if ("ponder.diagnostic.java_scene_no_ir".equals(key))
            format = "Java Ponder scenes do not have exportable script IR";
        else if ("ponder.diagnostic.ir_exported".equals(key))
            format = "Ponder IR exported to %1$s";
        else if ("ponder.diagnostic.ir_export_skipped".equals(key))
            format = "Ponder IR export skipped: Java scenes do not have script IR.";
        else if ("ponder.diagnostic.timeline_exported".equals(key))
            format = "Ponder timeline exported to %1$s";
        else if ("ponder.diagnostic.export_failed".equals(key))
            format = "Could not export Ponder scene %1$s: %2$s";
        else if ("ponder.diagnostic.dependencies_exported".equals(key))
            format = "Ponder structure dependencies: %1$s total, %2$s missing, %3$s error(s). Report: %4$s";
        else if ("ponder.diagnostic.dependencies_failed".equals(key))
            format = "Could not export Ponder structure dependencies: %1$s";
        else if ("ponder.diagnostic.summary".equals(key))
            format = "Ponder detected %1$s diagnostic issue(s) (%2$s error(s), %3$s warning(s)). Run /ponder validate for details.";
        else if ("ponder.diagnostic.command_failed".equals(key))
            format = "Ponder diagnostic command failed: %1$s";
        else if ("ponder.validation.complete".equals(key))
            format = "Ponder validation complete: %1$s error(s), %2$s warning(s). Report: %3$s";
        else if ("ponder.validation.report_failed".equals(key))
            format = "Ponder validation completed but the report could not be written: %1$s";
        else
            return key;
        return arguments.length == 0 ? format : String.format(Locale.ROOT, format, arguments);
    }
}
