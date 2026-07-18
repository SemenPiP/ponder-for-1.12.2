package net.createmod.ponder.script;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import org.junit.Test;

public class ZenScriptExamplesTest {
    @Test
    public void installablePackContainsOneActiveSceneAndDisabledFailures() throws Exception {
        File root = new File("examples/zenscript/scripts/ponder");
        File scenes = new File(root, "scenes");
        File active = new File(scenes, "ponder_zen_diagnostics.zs");
        assertTrue(active.isFile());
        File[] activeScripts = scenes.listFiles((directory, name) -> name.endsWith(".zs"));
        File[] disabledScripts = scenes.listFiles((directory, name) -> name.endsWith(".zs.disabled"));
        assertEquals(1, activeScripts == null ? 0 : activeScripts.length);
        assertEquals(4, disabledScripts == null ? 0 : disabledScripts.length);

        String script = new String(Files.readAllBytes(active.toPath()), StandardCharsets.UTF_8);
        for (String token : Arrays.asList("SharedText.register", "showSharedText",
            "showOutlineWithText", "showBoundingBox", "createItemEntity", "moveItem",
            "hideItem", "showItem", "removeItem", "scene.register()"))
            assertTrue("Missing example API call " + token, script.contains(token));
    }

    @Test
    public void externalExampleStructureMatchesTheShippedDemo() throws Exception {
        byte[] example = Files.readAllBytes(new File(
            "examples/zenscript/scripts/ponder/structures/ponder/demo/basics.nbt").toPath());
        byte[] shipped = Files.readAllBytes(new File(
            "src/main/resources/assets/ponder/ponder/demo/basics.nbt").toPath());
        assertArrayEquals(shipped, example);
    }
}
