package net.createmod.ponder.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Before;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.createmod.ponder.api.diagnostic.PonderSceneSource;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public class PonderJsonLoaderTest {
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    private File root;

    @Before
    public void setUp() throws Exception {
        root = temporary.newFolder("packs");
        PonderJsonLoader.setRoot(root);
        PonderJsonLoader.setReloadActionForTest(new Runnable() {
            @Override
            public void run() {
            }
        });
        PonderJsonLoader.reload();
    }

    @After
    public void tearDown() {
        PonderJsonLoader.setReloadActionForTest(null);
    }

    @Test
    public void loadsCompletePackIntoJsonLayer() throws Exception {
        write("demo.ponder.json", pack("test:json_demo", "JSON Demo", false));

        PonderJsonLoader.ReloadResult result = PonderJsonLoader.reload();

        assertEquals(1, result.packs);
        assertEquals(1, result.scenes);
        assertEquals(0, result.errors);
        List<ScriptSceneDefinition> scenes = ScriptSceneRegistry.jsonSnapshot();
        assertEquals(1, scenes.size());
        ScriptSceneDefinition scene = scenes.get(0);
        assertEquals(new ResourceLocation("test:json_demo"), scene.getSceneId());
        assertEquals(PonderSceneSource.LOCAL_JSON, scene.getLocalSource());
        assertTrue(scene.getSourceDescription().contains("scripts/ponder/packs/demo.ponder.json#/scenes/0"));
        assertEquals("show_section", scene.getInstructions().get(3).getOperation());
        assertEquals("layers_from", scene.getInstructions().get(3).getData()
            .getCompoundTag("selection").getString("type"));
        NBTTagCompound tileNbt = scene.getInstructions().get(4).getData().getCompoundTag("nbt");
        assertEquals("json", tileNbt.getString("Lock"));
        assertEquals(1, ScriptTagRegistry.jsonSnapshot().size());
        assertEquals("JSON step %s", ScriptSharedText.jsonSnapshot().get("json.step"));
        assertEquals(1, ScriptIndex.jsonSnapshot().size());
    }

    @Test
    public void invalidReloadRetainsLastKnownGoodUntilFileIsDeleted() throws Exception {
        File file = write("stable.ponder.json", pack("test:stable", "Stable", false));
        PonderJsonLoader.reload();
        Files.write(file.toPath(), "{broken".getBytes(StandardCharsets.UTF_8));

        PonderJsonLoader.ReloadResult stale = PonderJsonLoader.reload();

        assertEquals(1, stale.packs);
        assertEquals(1, stale.scenes);
        assertEquals(1, stale.errors);
        assertEquals(1, stale.warnings);
        assertEquals(new ResourceLocation("test:stable"),
            ScriptSceneRegistry.jsonSnapshot().get(0).getSceneId());

        assertTrue(file.delete());
        PonderJsonLoader.ReloadResult deleted = PonderJsonLoader.reload();
        assertEquals(0, deleted.packs);
        assertEquals(0, deleted.scenes);
        assertTrue(ScriptSceneRegistry.jsonSnapshot().isEmpty());
    }

    @Test
    public void staleFileDoesNotBlockUnrelatedValidPackUpdate() throws Exception {
        File stale = write("a.ponder.json",
            sceneOnlyPack("test:pack_a", "test:scene_a", "A1"));
        File changing = write("b.ponder.json",
            sceneOnlyPack("test:pack_b", "test:scene_b", "B1"));
        PonderJsonLoader.reload();

        Files.write(stale.toPath(), "{broken".getBytes(StandardCharsets.UTF_8));
        Files.write(changing.toPath(),
            sceneOnlyPack("test:pack_b", "test:scene_b", "B2")
                .getBytes(StandardCharsets.UTF_8));
        PonderJsonLoader.ReloadResult result = PonderJsonLoader.reload();

        assertEquals(2, result.packs);
        assertEquals(2, result.scenes);
        assertEquals(1, result.errors);
        assertEquals(1, result.warnings);
        assertEquals("A1", findJsonScene("test:scene_a").getTitle());
        assertEquals("B2", findJsonScene("test:scene_b").getTitle());
    }

    @Test
    public void rejectsUnknownInstructionFieldsWithoutBlockingOtherPack() throws Exception {
        write("good.ponder.json", pack("test:good", "Good", false));
        write("bad.ponder.json", pack("test:bad", "Bad", true));

        PonderJsonLoader.ReloadResult result = PonderJsonLoader.reload();

        assertEquals(1, result.packs);
        assertEquals(1, result.scenes);
        assertEquals(1, result.errors);
        assertEquals(new ResourceLocation("test:good"),
            ScriptSceneRegistry.jsonSnapshot().get(0).getSceneId());
    }

    @Test
    public void rejectsLaterDuplicateScenePackTransactionally() throws Exception {
        write("a.ponder.json", pack("test:duplicate", "First", false));
        write("b.ponder.json", pack("test:duplicate", "Second", false)
            .replace("\"id\": \"test:pack\"", "\"id\": \"test:pack_two\""));

        PonderJsonLoader.ReloadResult result = PonderJsonLoader.reload();

        assertEquals(1, result.packs);
        assertEquals(1, result.scenes);
        assertEquals(1, result.errors);
        assertEquals("First", ScriptSceneRegistry.jsonSnapshot().get(0).getTitle());
        assertFalse(ScriptSceneRegistry.jsonSnapshot().isEmpty());
    }

    @Test
    public void operationDescriptorAndSchemaMatchRuntimeContract() throws Exception {
        JsonObject descriptor = parseJson(new File("schemas/ponder-operations-v1.json"));
        assertEquals(1, descriptor.get("format").getAsInt());
        JsonObject operations = descriptor.getAsJsonObject("operations");

        Map<String, List<String>> runtimeFields = PonderJsonLoader.operationFieldsForTest();
        Map<String, List<String>> runtimeOptional = PonderJsonLoader.operationOptionalFieldsForTest();
        Set<String> descriptorOperations = new java.util.LinkedHashSet<String>();
        for (Map.Entry<String, JsonElement> operation : operations.entrySet())
            descriptorOperations.add(operation.getKey());
        assertEquals(runtimeFields.keySet(), descriptorOperations);

        for (Map.Entry<String, List<String>> runtime : runtimeFields.entrySet()) {
            JsonElement encoded = operations.get(runtime.getKey());
            List<String> descriptorFields;
            List<String> descriptorOptional = Collections.emptyList();
            if (encoded.isJsonArray()) {
                descriptorFields = strings(encoded);
            } else {
                JsonObject specification = encoded.getAsJsonObject();
                descriptorFields = strings(specification.get("fields"));
                descriptorOptional = strings(specification.get("optional"));
            }
            assertEquals(runtime.getKey(), runtime.getValue(), descriptorFields);
            assertEquals(runtime.getKey(), runtimeOptional.get(runtime.getKey()), descriptorOptional);
            assertTrue(runtime.getValue().containsAll(descriptorOptional));
        }

        JsonObject schema = parseJson(new File("schemas/ponder-pack-v1.schema.json"));
        assertEquals("http://json-schema.org/draft-07/schema#",
            schema.get("$schema").getAsString());
        JsonObject definitions = schema.getAsJsonObject("definitions");
        assertTrue(definitions.has("selection"));
        assertTrue(definitions.has("instruction"));
        assertTrue(definitions.has("scene"));
        assertTrue(definitions.has("tag"));
        JsonObject instructionProperties = definitions.getAsJsonObject("instruction")
            .getAsJsonObject("properties");
        Set<String> schemaOperations = new java.util.LinkedHashSet<String>(
            strings(instructionProperties.getAsJsonObject("op").get("enum")));
        assertEquals(runtimeFields.keySet(), schemaOperations);
        Set<String> runtimeInstructionFields = new java.util.LinkedHashSet<String>();
        for (List<String> fields : runtimeFields.values())
            runtimeInstructionFields.addAll(fields);
        Set<String> schemaInstructionFields = new java.util.LinkedHashSet<String>();
        for (Map.Entry<String, JsonElement> property : instructionProperties.entrySet())
            if (!"op".equals(property.getKey()))
                schemaInstructionFields.add(property.getKey());
        assertEquals(runtimeInstructionFields, schemaInstructionFields);
    }

    @Test
    public void rejectsInternalAliasesAndMissingRequiredFields() throws Exception {
        write("alias.ponder.json", pack("test:alias", "Alias", false)
            .replace("\"scene.idle\",\"ticks\":10", "\"idle\",\"ticks\":10"));
        PonderJsonLoader.ReloadResult alias = PonderJsonLoader.reload();
        assertEquals(1, alias.errors);
        assertTrue(ScriptSceneRegistry.jsonSnapshot().isEmpty());

        write("alias.ponder.json", pack("test:missing", "Missing", false)
            .replace("\"scene.idle\",\"ticks\":10", "\"scene.idle\""));
        PonderJsonLoader.ReloadResult missing = PonderJsonLoader.reload();
        assertEquals(1, missing.errors);
        assertTrue(ScriptSceneRegistry.jsonSnapshot().isEmpty());
    }

    @Test
    public void jsonCannotOverrideZenScriptScene() throws Exception {
        ResourceLocation sceneId = new ResourceLocation("test", "zs_conflict");
        ScriptSceneRegistry.register(new ScriptSceneDefinition(
            new ResourceLocation("minecraft", "paper"), sceneId, "ZenScript",
            new ResourceLocation("ponder", "demo/basics"),
            Collections.<ResourceLocation>emptyList(),
            Collections.singletonList(new ScriptInstruction("finish", null)), false));
        write("conflict.ponder.json", pack(sceneId.toString(), "JSON", false));

        PonderJsonLoader.ReloadResult result = PonderJsonLoader.reload();

        assertEquals(1, result.errors);
        assertTrue(ScriptSceneRegistry.jsonSnapshot().isEmpty());
        assertEquals("ZenScript", ScriptSceneRegistry.find(
            net.createmod.ponder.api.diagnostic.PonderDiagnosticView.LOCAL, sceneId).getTitle());
    }

    @Test
    public void failedRegistrationReloadRollsBackAllJsonLayers() throws Exception {
        write("stable.ponder.json", pack("test:rollback_old", "Old", false));
        PonderJsonLoader.reload();
        List<ScriptSceneDefinition> oldScenes = ScriptSceneRegistry.jsonSnapshot();
        CollectionSnapshot old = new CollectionSnapshot(
            ScriptTagRegistry.jsonSnapshot().size(),
            ScriptSharedText.jsonSnapshot().get("json.step"),
            ScriptIndex.jsonSnapshot().size());

        write("stable.ponder.json", pack("test:rollback_new", "New", false));
        PonderJsonLoader.setReloadActionForTest(new Runnable() {
            @Override
            public void run() {
                throw new IllegalStateException("registration failed");
            }
        });
        try {
            PonderJsonLoader.reload();
            throw new AssertionError("Failed registration reload was accepted");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("registration failed"));
        }

        assertEquals(oldScenes.get(0).getSceneId(),
            ScriptSceneRegistry.jsonSnapshot().get(0).getSceneId());
        assertEquals(old.tags, ScriptTagRegistry.jsonSnapshot().size());
        assertEquals(old.sharedText, ScriptSharedText.jsonSnapshot().get("json.step"));
        assertEquals(old.exclusions, ScriptIndex.jsonSnapshot().size());
    }

    @Test
    public void localSnapshotCarriesJsonScenesTagsAndSharedText() throws Exception {
        write("sync.ponder.json", pack("test:json_sync", "Sync", false));
        PonderJsonLoader.reload();

        ScriptSceneSnapshot.Encoded encoded = ScriptSceneSnapshot.encodeLocal(
            ScriptSceneRegistry.localSnapshot(false));
        ScriptSceneSnapshot.Decoded decoded =
            ScriptSceneSnapshot.decodeContent(encoded.bytes, encoded.uncompressedBytes);

        boolean foundScene = false;
        for (ScriptSceneDefinition scene : decoded.scenes)
            if (scene.getSceneId().equals(new ResourceLocation("test", "json_sync")))
                foundScene = true;
        assertTrue(foundScene);
        boolean foundTag = false;
        for (ScriptTagDefinition tag : decoded.tags)
            if (tag.id.equals(new ResourceLocation("test", "json")))
                foundTag = true;
        assertTrue(foundTag);
        assertEquals("JSON step %s", decoded.sharedText.get("json.step"));
    }

    @Test
    public void coercesJsonNumbersToTheIrFieldTypes() throws Exception {
        write("numbers.ponder.json", numericPack());

        PonderJsonLoader.ReloadResult result = PonderJsonLoader.reload();

        assertEquals(0, result.errors);
        ScriptSceneDefinition scene = findJsonScene("test:numeric_types");
        assertTrue(scene.getInstructions().get(0).getData().hasKey("value", 5));
        assertTrue(scene.getInstructions().get(1).getData().hasKey("x", 6));
        assertTrue(scene.getInstructions().get(2).getData().hasKey("x", 6));
        assertTrue(scene.getInstructions().get(2).getData().hasKey("mx", 6));
        assertTrue(scene.getInstructions().get(4).getData().hasKey("amount", 5));
    }

    @Test
    public void installableExamplePackUsesTheRuntimeLoader() {
        PonderJsonLoader.setRoot(new File(
            "examples/json/scripts/ponder/packs"));

        PonderJsonLoader.ReloadResult result = PonderJsonLoader.reload();

        assertEquals(1, result.packs);
        assertEquals(1, result.scenes);
        assertEquals(0, result.errors);
        ScriptSceneDefinition scene = findJsonScene("ponder_json:crafting_demo");
        assertEquals(PonderSceneSource.LOCAL_JSON, scene.getLocalSource());
        assertTrue(scene.getInstructions().stream()
            .anyMatch(instruction -> "create_item".equals(instruction.getOperation())));
        assertTrue(scene.getInstructions().stream()
            .anyMatch(instruction -> "tile_nbt".equals(instruction.getOperation())));
    }

    private static JsonObject parseJson(File file) throws Exception {
        try (FileReader reader = new FileReader(file)) {
            return new JsonParser().parse(reader).getAsJsonObject();
        }
    }

    private static List<String> strings(JsonElement value) {
        List<String> result = new ArrayList<String>();
        for (JsonElement entry : value.getAsJsonArray())
            result.add(entry.getAsString());
        return result;
    }

    private static final class CollectionSnapshot {
        final int tags;
        final String sharedText;
        final int exclusions;

        CollectionSnapshot(int tags, String sharedText, int exclusions) {
            this.tags = tags;
            this.sharedText = sharedText;
            this.exclusions = exclusions;
        }
    }

    private File write(String name, String contents) throws Exception {
        File file = new File(root, name);
        Files.write(file.toPath(), contents.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private static ScriptSceneDefinition findJsonScene(String id) {
        ResourceLocation sceneId = new ResourceLocation(id);
        for (ScriptSceneDefinition scene : ScriptSceneRegistry.jsonSnapshot())
            if (sceneId.equals(scene.getSceneId()))
                return scene;
        throw new AssertionError("Missing JSON scene " + id);
    }

    private static String sceneOnlyPack(String packId, String sceneId, String title) {
        return "{\n"
            + "  \"format\": 1,\n"
            + "  \"id\": \"" + packId + "\",\n"
            + "  \"scenes\": [{\n"
            + "    \"id\": \"" + sceneId + "\",\n"
            + "    \"component\": \"minecraft:paper\",\n"
            + "    \"title\": \"" + title + "\",\n"
            + "    \"structure\": \"ponder:demo/basics\",\n"
            + "    \"instructions\": [{\"op\":\"scene.finish\"}]\n"
            + "  }]\n"
            + "}\n";
    }

    private static String numericPack() {
        return "{\n"
            + "  \"format\": 1,\n"
            + "  \"id\": \"test:numeric_types\",\n"
            + "  \"scenes\": [{\n"
            + "    \"id\": \"test:numeric_types\",\n"
            + "    \"component\": \"minecraft:paper\",\n"
            + "    \"title\": \"Numeric types\",\n"
            + "    \"structure\": \"ponder:demo/basics\",\n"
            + "    \"instructions\": [\n"
            + "      {\"op\":\"scene.scale\",\"value\":1},\n"
            + "      {\"op\":\"scene.move_poi\",\"x\":1,\"y\":2,\"z\":3},\n"
            + "      {\"op\":\"world.create_item\",\"handle\":\"item\",\"x\":1,\"y\":2,\"z\":3,"
            + "\"mx\":0,\"my\":0,\"mz\":0,\"item\":\"minecraft:paper\",\"count\":1,\"meta\":0},\n"
            + "      {\"op\":\"world.move_item\",\"handle\":\"item\",\"x\":1,\"y\":0,\"z\":0,"
            + "\"duration\":10},\n"
            + "      {\"op\":\"effects.particles\",\"x\":1,\"y\":2,\"z\":3,"
            + "\"type\":\"smoke_normal\",\"mx\":0,\"my\":0,\"mz\":0,\"amount\":1,\"cycles\":1},\n"
            + "      {\"op\":\"scene.finish\"}\n"
            + "    ]\n"
            + "  }]\n"
            + "}\n";
    }

    private static String pack(String sceneId, String title, boolean unknownInstructionField) {
        return "{\n"
            + "  \"format\": 1,\n"
            + "  \"id\": \"test:pack\",\n"
            + "  \"tags\": [{\n"
            + "    \"id\": \"test:json\", \"icon\": \"minecraft:book\",\n"
            + "    \"title\": \"JSON\", \"description\": \"JSON scenes\",\n"
            + "    \"indexed\": true, \"components\": [\"minecraft:crafting_table\"]\n"
            + "  }],\n"
            + "  \"sharedText\": {\"json.step\": \"JSON step %s\"},\n"
            + "  \"indexExclusions\": [\"minecraft:barrier\"],\n"
            + "  \"scenes\": [{\n"
            + "    \"id\": \"" + sceneId + "\",\n"
            + "    \"component\": \"minecraft:crafting_table\",\n"
            + "    \"title\": \"" + title + "\",\n"
            + "    \"structure\": \"ponder:demo/basics\",\n"
            + "    \"tags\": [\"test:json\"],\n"
            + "    \"instructions\": [\n"
            + "      {\"op\":\"scene.configure_base_plate\",\"x\":0,\"z\":0,\"size\":5},\n"
            + "      {\"op\":\"scene.show_base_plate\"},\n"
            + "      {\"op\":\"scene.idle\",\"ticks\":10"
            + (unknownInstructionField ? ",\"unexpected\":true" : "") + "},\n"
            + "      {\"op\":\"world.show_section\",\"selection\":{\"type\":\"layers_from\",\"y\":1},"
            + "\"direction\":\"down\"},\n"
            + "      {\"op\":\"world.tile_nbt\",\"selection\":{\"type\":\"position\",\"pos\":[3,1,2]},"
            + "\"nbt\":\"{Lock:\\\"json\\\"}\",\"replace\":false,\"redraw\":false},\n"
            + "      {\"op\":\"scene.finish\"}\n"
            + "    ]\n"
            + "  }]\n"
            + "}\n";
    }
}
