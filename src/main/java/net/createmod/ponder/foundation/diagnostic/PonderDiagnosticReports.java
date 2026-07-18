package net.createmod.ponder.foundation.diagnostic;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.createmod.ponder.api.diagnostic.PonderDiagnosticIssue;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticSnapshot;
import net.createmod.ponder.api.diagnostic.PonderSceneDiagnostic;
import net.createmod.ponder.api.diagnostic.PonderStructureDependency;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.script.ScriptInstruction;
import net.createmod.ponder.script.ScriptSceneDefinition;
import net.minecraftforge.fml.common.Loader;

public final class PonderDiagnosticReports {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private PonderDiagnosticReports() {
    }

    public static File writeValidation(PonderDiagnosticSnapshot snapshot) throws IOException {
        JsonObject root = snapshotJson(snapshot);
        return writeJson("validate-" + snapshot.getView().name().toLowerCase(Locale.ROOT), root);
    }

    public static File writeDependencies(net.createmod.ponder.api.diagnostic.PonderDiagnosticView view,
                                         java.util.List<PonderStructureDependency> dependencies)
            throws IOException {
        return writeJson("structure-dependencies-" + view.name().toLowerCase(Locale.ROOT),
            dependenciesJson(view, dependencies));
    }

    static JsonObject dependenciesJson(
            net.createmod.ponder.api.diagnostic.PonderDiagnosticView view,
            java.util.List<PonderStructureDependency> dependencies) {
        JsonObject root = new JsonObject();
        root.addProperty("format", 1);
        root.addProperty("view", view.name());
        root.addProperty("createdAt", System.currentTimeMillis());
        JsonArray structures = new JsonArray();
        for (PonderStructureDependency dependency : dependencies) {
            JsonObject value = new JsonObject();
            value.addProperty("structure", dependency.getStructureId().toString());
            if (dependency.getProviderId() != null)
                value.addProperty("provider", dependency.getProviderId().toString());
            value.addProperty("fingerprint", dependency.getFingerprint());
            value.addProperty("status", dependency.getStatus().name());
            if (dependency.getContributorId() != null)
                value.addProperty("contributor", dependency.getContributorId().toString());
            value.add("scenes", resourceIds(dependency.getSceneIds()));
            value.add("components", resourceIds(dependency.getComponents()));
            JsonArray sources = new JsonArray();
            for (net.createmod.ponder.api.diagnostic.PonderSceneSource source : dependency.getSources())
                sources.add(source.name());
            value.add("sources", sources);
            structures.add(value);
        }
        root.add("structures", structures);
        return root;
    }

    public static File writeTimeline(PonderSceneDiagnostic scene, ScriptSceneDefinition definition)
            throws IOException {
        return writeTimeline(scene, definition,
            java.util.Collections.<PonderScene.ScheduledInstructionDiagnostic>emptyList());
    }

    public static File writeTimeline(PonderSceneDiagnostic scene, ScriptSceneDefinition definition,
                                     java.util.List<PonderScene.ScheduledInstructionDiagnostic> javaTimeline)
            throws IOException {
        JsonObject root = sceneJson(scene);
        JsonArray timeline = new JsonArray();
        int cursor = 0;
        if (definition != null) {
            int index = 0;
            for (ScriptInstruction instruction : definition.getInstructions()) {
                JsonObject row = new JsonObject();
                row.addProperty("index", index++);
                row.addProperty("operation", instruction.getOperation());
                row.addProperty("startTick", cursor);
                int duration = instruction.getData().hasKey("duration")
                    ? Math.max(0, instruction.getData().getInteger("duration")) : 0;
                row.addProperty("duration", duration);
                row.addProperty("keyframe", scene.getKeyframes().contains(cursor));
                timeline.add(row);
                if ("idle".equals(instruction.getOperation()))
                    cursor += Math.max(0, instruction.getData().getInteger("ticks"));
            }
        } else {
            int index = 0;
            for (PonderScene.ScheduledInstructionDiagnostic instruction : javaTimeline) {
                JsonObject row = new JsonObject();
                row.addProperty("index", index++);
                row.addProperty("operation", instruction.getInstructionType());
                row.addProperty("startTick", instruction.getStartTick());
                row.addProperty("duration", instruction.getDuration());
                row.addProperty("keyframe", scene.getKeyframes().contains(instruction.getStartTick()));
                timeline.add(row);
            }
            if (timeline.size() == 0) {
                JsonObject row = new JsonObject();
                row.addProperty("index", 0);
                row.addProperty("operation", "java_storyboard");
                row.addProperty("startTick", 0);
                row.addProperty("duration", scene.getTotalTicks());
                row.addProperty("keyframe", scene.getKeyframes().contains(0));
                timeline.add(row);
            }
        }
        root.add("timeline", timeline);
        return writeJson("timeline-" + safeScene(scene), root);
    }

    public static File writeIr(PonderSceneDiagnostic scene, ScriptSceneDefinition definition)
            throws IOException {
        if (definition == null)
            throw new IllegalArgumentException("Java storyboard scenes do not have deterministic script IR");
        File target = target("ir-" + safeScene(scene), ".snbt");
        Files.write(target.toPath(), definition.serialize().toString().getBytes(StandardCharsets.UTF_8));
        return target;
    }

    static JsonObject snapshotJson(PonderDiagnosticSnapshot snapshot) {
        JsonObject root = new JsonObject();
        root.addProperty("format", 1);
        root.addProperty("view", snapshot.getView().name());
        root.addProperty("generation", snapshot.getGeneration());
        root.addProperty("createdAt", snapshot.getCreatedAt());
        JsonArray scenes = new JsonArray();
        for (PonderSceneDiagnostic scene : snapshot.getScenes())
            scenes.add(sceneJson(scene));
        root.add("scenes", scenes);
        JsonArray issues = new JsonArray();
        for (PonderDiagnosticIssue issue : snapshot.getIssues())
            issues.add(issueJson(issue));
        root.add("issues", issues);
        return root;
    }

    private static JsonObject sceneJson(PonderSceneDiagnostic scene) {
        JsonObject value = new JsonObject();
        value.addProperty("entryKey", scene.getEntryKey());
        if (scene.getSceneId() != null)
            value.addProperty("sceneId", scene.getSceneId().toString());
        value.addProperty("component", scene.getComponent().toString());
        value.addProperty("structure", scene.getStructure().toString());
        value.addProperty("title", scene.getTitle());
        value.addProperty("source", scene.getSource().name());
        value.addProperty("sourceDescription", scene.getSourceDescription());
        value.addProperty("pluginId", scene.getPluginId());
        if (scene.getOverriddenBy() != null)
            value.addProperty("overriddenBy", scene.getOverriddenBy().name());
        if (scene.getProviderId() != null)
            value.addProperty("providerId", scene.getProviderId().toString());
        value.addProperty("fingerprint", scene.getFingerprint());
        value.addProperty("instructionCount", scene.getInstructionCount());
        value.addProperty("totalTicks", scene.getTotalTicks());
        JsonArray tags = new JsonArray();
        for (net.minecraft.util.ResourceLocation tag : scene.getTags())
            tags.add(tag.toString());
        value.add("tags", tags);
        JsonArray keyframes = new JsonArray();
        for (Integer keyframe : scene.getKeyframes())
            keyframes.add(keyframe);
        value.add("keyframes", keyframes);
        JsonArray issues = new JsonArray();
        for (PonderDiagnosticIssue issue : scene.getIssues())
            issues.add(issueJson(issue));
        value.add("issues", issues);
        return value;
    }

    private static JsonObject issueJson(PonderDiagnosticIssue issue) {
        JsonObject entry = new JsonObject();
        entry.addProperty("code", issue.getCode());
        entry.addProperty("severity", issue.getSeverity().name());
        entry.addProperty("message", issue.getMessage());
        if (issue.getSceneId() != null)
            entry.addProperty("sceneId", issue.getSceneId().toString());
        if (issue.hasInstructionIndex())
            entry.addProperty("instructionIndex", issue.getInstructionIndex());
        return entry;
    }

    private static JsonArray resourceIds(
            java.util.Collection<net.minecraft.util.ResourceLocation> ids) {
        JsonArray result = new JsonArray();
        for (net.minecraft.util.ResourceLocation id : ids)
            result.add(id.toString());
        return result;
    }

    private static File writeJson(String name, JsonObject value) throws IOException {
        File target = target(name, ".json");
        Files.write(target.toPath(), GSON.toJson(value).getBytes(StandardCharsets.UTF_8));
        return target;
    }

    private static File target(String name, String extension) throws IOException {
        File configDirectory = Loader.instance().getConfigDir();
        File gameDirectory = configDirectory == null || configDirectory.getParentFile() == null
            ? new File(System.getProperty("user.dir", "."))
            : configDirectory.getParentFile();
        File directory = new File(gameDirectory, "logs/ponder/diagnostics");
        if (!directory.isDirectory() && !directory.mkdirs())
            throw new IOException("Could not create Ponder diagnostic directory " + directory);
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss'Z'", Locale.ROOT);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return new File(directory, format.format(new Date()) + "-" + name + extension);
    }

    private static String safeScene(PonderSceneDiagnostic scene) {
        String value = scene.getSceneId() == null ? scene.getEntryKey() : scene.getSceneId().toString();
        return value.replaceAll("[^A-Za-z0-9_.-]", "_");
    }
}
