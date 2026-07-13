package net.createmod.ponder.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class BuiltinScriptInstallerTest {
    @Rule public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void generatesOnceAndDoesNotRestoreDeletedScripts() throws Exception {
        File game = temporary.newFolder("game");
        BuiltinScriptInstaller.installOnce(game);
        File builtin = new File(game, "scripts/ponder/builtin");
        assertEquals(8, builtin.listFiles((dir, name) -> name.endsWith(".zs")).length);
        File basics = new File(builtin, "basics.zs");
        assertTrue(basics.isFile());
        Files.delete(basics.toPath());

        BuiltinScriptInstaller.installOnce(game);
        assertFalse("Generated scripts must not be restored after the marker exists", basics.exists());
        assertTrue(new File(game, "config/ponder/builtin-zs-generated.properties").isFile());
    }

    @Test
    public void bundledScriptsUseStaticApiAndThirtyTwoSecondTimeline() throws Exception {
        String[] names = {"basics", "storage", "smelting", "piston", "redstone", "render_layers", "fluids", "rail"};
        for (String name : names) {
            byte[] bytes = read("assets/ponder/scripts/builtin/" + name + ".zs");
            String script = new String(bytes, StandardCharsets.UTF_8);
            assertTrue(script.contains("SceneRegistry.create"));
            assertTrue(script.contains("scene.register();"));
            int total = 0;
            java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile("scene\\.idle\\((\\d+)\\)").matcher(script);
            while (matcher.find()) total += Integer.parseInt(matcher.group(1));
            assertEquals("Built-in script must schedule exactly 32 seconds: " + name, 640, total);

            java.util.regex.Matcher timeline = java.util.regex.Pattern
                .compile("scene\\.idle\\((\\d+)\\)|scene\\.addKeyframe\\(\\)")
                .matcher(script);
            int elapsed = 0;
            int keyframes = 0;
            int[] expected = {40, 190, 340, 490};
            while (timeline.find()) {
                if (timeline.group(1) != null) {
                    elapsed += Integer.parseInt(timeline.group(1));
                } else {
                    assertTrue("Too many keyframes: " + name, keyframes < expected.length);
                    assertEquals("Unexpected keyframe time: " + name, expected[keyframes], elapsed);
                    keyframes++;
                }
            }
            assertEquals("Built-in script must declare four keyframes: " + name, 4, keyframes);
            assertEquals("Keyframe timeline must end at 640 ticks: " + name, 640, elapsed);
        }
    }

    private static byte[] read(String path) throws Exception {
        java.io.InputStream stream = BuiltinScriptInstallerTest.class.getClassLoader().getResourceAsStream(path);
        if (stream == null) throw new AssertionError("Missing " + path);
        try {
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = stream.read(buffer)) >= 0) output.write(buffer, 0, read);
            return output.toByteArray();
        } finally {
            stream.close();
        }
    }
}
