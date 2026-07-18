package net.createmod.ponder.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

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

    private File write(String name, String contents) throws Exception {
        File file = new File(root, name);
        Files.write(file.toPath(), contents.getBytes(StandardCharsets.UTF_8));
        return file;
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
