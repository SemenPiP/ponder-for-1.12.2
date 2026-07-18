package net.createmod.ponder.foundation.diagnostic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import net.createmod.ponder.api.diagnostic.PonderDiagnosticIssue;
import net.createmod.ponder.api.diagnostic.PonderSceneDiagnostic;
import net.createmod.ponder.api.diagnostic.PonderSceneSource;
import net.minecraft.util.ResourceLocation;

public class PonderDiagnosticRegistryTest {
    @Test
    public void clientEffectiveViewOverridesOnlyLocalScriptScenes() {
        PonderSceneDiagnostic localScript = scene("shared", PonderSceneSource.LOCAL_ZS);
        PonderSceneDiagnostic localJava = scene("java", PonderSceneSource.JAVA_PLUGIN);
        PonderSceneDiagnostic server = scene("shared", PonderSceneSource.SERVER_SNAPSHOT);

        PonderDiagnosticRegistry.ClientViews merged = PonderDiagnosticRegistry.mergeClientViews(
            Arrays.asList(localScript, localJava), Collections.singletonList(server));

        assertTrue(merged.local.get(0).isOverridden());
        assertEquals(PonderSceneSource.SERVER_SNAPSHOT, merged.local.get(0).getOverriddenBy());
        assertTrue(merged.local.get(0).getIssues().stream()
            .map(PonderDiagnosticIssue::getCode).anyMatch("override.server_scene"::equals));
        assertEquals(2, merged.effective.size());
        assertTrue(merged.effective.contains(localJava));
        assertTrue(merged.effective.contains(server));
        assertFalse(merged.effective.contains(localScript));
    }

    private static PonderSceneDiagnostic scene(String path, PonderSceneSource source) {
        ResourceLocation id = new ResourceLocation("test", path);
        return new PonderSceneDiagnostic(source.name() + ":" + path, id,
            new ResourceLocation("test", "component"), new ResourceLocation("test", "structure"),
            path, source, "", "test", null, "", Collections.<ResourceLocation>emptyList(),
            1, 20, Collections.<Integer>emptyList(),
            Collections.<PonderDiagnosticIssue>emptyList(), null);
    }
}
