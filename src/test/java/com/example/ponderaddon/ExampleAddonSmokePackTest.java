package com.example.ponderaddon;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

public class ExampleAddonSmokePackTest {
    @Test
    public void smokeScriptUsesVersionedCodecAndServerMetadata() throws Exception {
        File script = new File("examples/addon/smoke/scripts/ponder/scenes/ponder_example_codec.zs");
        assertTrue(script.isFile());
        String source = new String(Files.readAllBytes(script.toPath()), StandardCharsets.UTF_8);
        assertTrue(source.contains("scene.custom(\"ponder_example:pulse\""));
        assertTrue(source.contains("TagRegistry.create(\"ponder_example:codec\""));
        assertTrue(source.contains("SharedText.register(\"ponder_example.pulse\""));
        assertTrue(source.contains("\"ponder_example:codec_demo\""));
    }

    @Test
    public void serviceLoaderAndImcPluginsDeclareStableSceneIds() throws Exception {
        String service = new String(Files.readAllBytes(new File(
            "examples/addon/src/main/java/com/example/ponderaddon/ExamplePonderPlugin.java").toPath()),
            StandardCharsets.UTF_8);
        String imc = new String(Files.readAllBytes(new File(
            "examples/addon/src/main/java/com/example/ponderaddon/ExampleImcPonderPlugin.java").toPath()),
            StandardCharsets.UTF_8);
        assertTrue(service.contains("identifiedBy(id(\"service_loader\"))"));
        assertTrue(imc.contains("identifiedBy(id(\"imc_registration\"))"));
    }
}
