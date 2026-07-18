package net.createmod.ponder.verification;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Stream;

import net.createmod.ponder.api.diagnostic.PonderDiagnosticView;
import net.createmod.ponder.api.diagnostic.PonderSceneSource;
import net.createmod.ponder.command.SimplePonderActions;
import net.createmod.ponder.script.PonderJsonLoader;
import net.createmod.ponder.script.ScriptInstruction;
import net.createmod.ponder.script.ScriptSceneDefinition;
import net.createmod.ponder.script.ScriptSceneRegistry;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

/** Isolated runtime fixture for the reloadable JSON scene layer. */
final class JsonReloadHarness {
    static final ResourceLocation COMPONENT = new ResourceLocation("minecraft", "paper");
    static final ResourceLocation SCENE = new ResourceLocation("ponder_harness", "json_reload");

    private static final ResourceLocation BUILTIN_SCENE =
        new ResourceLocation("ponder", "ponder_basics");

    private final File root;
    private final File stablePack;
    private final File conflictPack;

    JsonReloadHarness(File outputDirectory) {
        root = new File(outputDirectory, "json-runtime-fixture");
        stablePack = new File(root, "stable.ponder.json");
        conflictPack = new File(root, "conflict.ponder.json");
    }

    String installInitial() throws IOException {
        resetRoot();
        write(stablePack, pack("ponder_harness:runtime_pack", SCENE.toString(),
            "JSON Runtime V1", "harness_v1"));
        PonderJsonLoader.setRoot(root);
        PonderJsonLoader.ReloadResult result = PonderJsonLoader.reload();
        require(result.packs == 1 && result.scenes == 1 && result.warnings == 0 && result.errors == 0,
            "initial JSON reload result was " + describe(result));
        assertLocal("JSON Runtime V1", "harness_v1");
        return "root=" + root.getAbsolutePath() + ", " + describe(result);
    }

    String assertInitialSceneDefinition() {
        ScriptSceneDefinition definition = assertLocal("JSON Runtime V1", "harness_v1");
        return "scene=" + definition.getSceneId() + ", instructions="
            + definition.getInstructions().size() + ", source=" + definition.getSourceDescription();
    }

    String retainLastKnownGood() throws IOException {
        write(stablePack, "{broken");
        PonderJsonLoader.ReloadResult result = PonderJsonLoader.reload();
        require(result.packs == 1 && result.scenes == 1 && result.warnings == 1 && result.errors == 1,
            "last-known-good reload result was " + describe(result));
        assertLocal("JSON Runtime V1", "harness_v1");
        return describe(result) + ", retained=JSON Runtime V1";
    }

    String updateToSecondVersion() throws IOException {
        write(stablePack, pack("ponder_harness:runtime_pack", SCENE.toString(),
            "JSON Runtime V2", "harness_v2"));
        PonderJsonLoader.ReloadResult result = PonderJsonLoader.reload();
        require(result.packs == 1 && result.scenes == 1 && result.warnings == 0 && result.errors == 0,
            "updated JSON reload result was " + describe(result));
        assertLocal("JSON Runtime V2", "harness_v2");
        return describe(result) + ", title=JSON Runtime V2";
    }

    String isolateZenScriptConflict() throws IOException {
        write(conflictPack, pack("ponder_harness:conflict_pack", BUILTIN_SCENE.toString(),
            "Must Not Replace ZenScript", "conflict"));
        PonderJsonLoader.ReloadResult result = PonderJsonLoader.reload();
        require(result.packs == 1 && result.scenes == 1 && result.errors == 1,
            "ZenScript conflict reload result was " + describe(result));
        assertLocal("JSON Runtime V2", "harness_v2");
        ScriptSceneDefinition builtin = ScriptSceneRegistry.find(PonderDiagnosticView.LOCAL, BUILTIN_SCENE);
        require(builtin != null, "built-in ZenScript scene disappeared after JSON conflict");
        require(builtin.getLocalSource() != PonderSceneSource.LOCAL_JSON,
            "JSON replaced the built-in ZenScript scene");
        return describe(result) + ", protected=" + BUILTIN_SCENE;
    }

    String installServerOverride() {
        ScriptSceneDefinition local = assertLocal("JSON Runtime V2", "harness_v2");
        ScriptSceneDefinition server = new ScriptSceneDefinition(
            local.getComponent(), local.getSceneId(), "Server Override",
            local.getStructure(), local.getTags(), local.getInstructions(), false);
        ScriptSceneRegistry.replaceServerScenesAndReload(Collections.singletonList(server));
        require("JSON Runtime V2".equals(scene(PonderDiagnosticView.LOCAL).getTitle()),
            "server layer changed the local JSON scene");
        require("Server Override".equals(scene(PonderDiagnosticView.SERVER).getTitle()),
            "server scene was not installed");
        require("Server Override".equals(scene(PonderDiagnosticView.EFFECTIVE).getTitle()),
            "server scene did not override the effective view");
        return "local=JSON Runtime V2, server/effective=Server Override";
    }

    String reloadClearsServerLayer() {
        SimplePonderActions.reloadPonder("");
        require(ScriptSceneRegistry.serverSnapshot().isEmpty(),
            "client reload retained the server scene layer");
        assertLocal("JSON Runtime V2", "harness_v2");
        require("JSON Runtime V2".equals(scene(PonderDiagnosticView.EFFECTIVE).getTitle()),
            "client reload did not restore the local JSON scene");
        return "server layer cleared; local JSON scene restored";
    }

    String deletePack() throws IOException {
        Files.deleteIfExists(stablePack.toPath());
        Files.deleteIfExists(conflictPack.toPath());
        PonderJsonLoader.ReloadResult result = PonderJsonLoader.reload();
        require(result.packs == 0 && result.scenes == 0,
            "deleted JSON pack remained registered: " + describe(result));
        require(ScriptSceneRegistry.find(PonderDiagnosticView.LOCAL, SCENE) == null,
            "deleted JSON scene remained in the local layer");
        return describe(result) + ", deleted=" + SCENE;
    }

    private ScriptSceneDefinition assertLocal(String title, String lock) {
        ScriptSceneDefinition definition = scene(PonderDiagnosticView.LOCAL);
        require(definition.getLocalSource() == PonderSceneSource.LOCAL_JSON,
            "runtime scene source was " + definition.getLocalSource());
        require(title.equals(definition.getTitle()),
            "runtime scene title was " + definition.getTitle() + " instead of " + title);
        NBTTagCompound tileNbt = null;
        for (ScriptInstruction instruction : definition.getInstructions()) {
            if ("tile_nbt".equals(instruction.getOperation()))
                tileNbt = instruction.getData().getCompoundTag("nbt");
        }
        require(tileNbt != null && lock.equals(tileNbt.getString("Lock")),
            "runtime scene SNBT lock did not match " + lock);
        return definition;
    }

    private static ScriptSceneDefinition scene(PonderDiagnosticView view) {
        ScriptSceneDefinition definition = ScriptSceneRegistry.find(view, SCENE);
        require(definition != null, "missing " + view + " JSON runtime scene " + SCENE);
        return definition;
    }

    private void resetRoot() throws IOException {
        Path rootPath = root.toPath().toAbsolutePath().normalize();
        if (Files.exists(rootPath)) {
            try (Stream<Path> paths = Files.walk(rootPath)) {
                paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        if (path.equals(rootPath))
                            return;
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException failure) {
                            throw new FixtureIoException(failure);
                        }
                    });
            }
        }
        Files.createDirectories(rootPath);
    }

    private static void write(File file, String value) throws IOException {
        Files.createDirectories(file.toPath().getParent());
        Files.write(file.toPath(), value.getBytes(StandardCharsets.UTF_8));
    }

    private static String pack(String packId, String sceneId, String title, String lock) {
        return "{\n"
            + "  \"format\": 1,\n"
            + "  \"id\": \"" + packId + "\",\n"
            + "  \"sharedText\": {\"ponder_harness:runtime\": \"JSON runtime %s\"},\n"
            + "  \"scenes\": [{\n"
            + "    \"id\": \"" + sceneId + "\",\n"
            + "    \"component\": \"" + COMPONENT + "\",\n"
            + "    \"title\": \"" + title + "\",\n"
            + "    \"structure\": \"ponder:demo/basics\",\n"
            + "    \"instructions\": [\n"
            + "      {\"op\":\"scene.configure_base_plate\",\"x\":0,\"z\":0,\"size\":5},\n"
            + "      {\"op\":\"scene.show_base_plate\"},\n"
            + "      {\"op\":\"world.show_section\",\"selection\":{\"type\":\"layers_from\",\"y\":1},"
            + "\"direction\":\"down\"},\n"
            + "      {\"op\":\"world.tile_nbt\",\"selection\":{\"type\":\"position\",\"pos\":[3,1,2]},"
            + "\"nbt\":\"{Lock:\\\"" + lock + "\\\"}\",\"replace\":false,\"redraw\":true},\n"
            + "      {\"op\":\"overlay.show_shared_text\",\"duration\":40,"
            + "\"key\":\"ponder_harness:runtime\",\"params\":[\"" + lock + "\"],"
            + "\"x\":2.5,\"y\":2.5,\"z\":2.5,\"color\":\"white\","
            + "\"near\":true,\"keyframe\":true},\n"
            + "      {\"op\":\"scene.idle\",\"ticks\":20},\n"
            + "      {\"op\":\"scene.finish\"}\n"
            + "    ]\n"
            + "  }]\n"
            + "}\n";
    }

    private static String describe(PonderJsonLoader.ReloadResult result) {
        return "packs=" + result.packs + ", scenes=" + result.scenes
            + ", warnings=" + result.warnings + ", errors=" + result.errors;
    }

    private static void require(boolean condition, String message) {
        if (!condition)
            throw new IllegalStateException(message);
    }

    private static final class FixtureIoException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        FixtureIoException(IOException cause) {
            super(cause);
        }
    }
}
