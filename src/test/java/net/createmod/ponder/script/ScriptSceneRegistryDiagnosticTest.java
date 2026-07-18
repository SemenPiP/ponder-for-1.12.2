package net.createmod.ponder.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import net.createmod.ponder.api.diagnostic.PonderDiagnosticIssue;
import net.minecraft.util.ResourceLocation;

public class ScriptSceneRegistryDiagnosticTest {
    @Test
    public void registrationFailuresRetainSceneAndNormalizedSource() {
        ScriptSceneRegistry.drainRegistrationIssues();
        ResourceLocation sceneId = new ResourceLocation("test", "duplicate");
        ScriptSceneRegistry.recordRegistrationFailure(sceneId,
            "scripts/ponder/scenes/test.zs:12", new IllegalArgumentException("duplicate id"));

        List<PonderDiagnosticIssue> issues = ScriptSceneRegistry.drainRegistrationIssues();
        assertEquals(1, issues.size());
        assertEquals("registration.script_failed", issues.get(0).getCode());
        assertEquals(sceneId, issues.get(0).getSceneId());
        assertTrue(issues.get(0).getMessage().contains("scripts/ponder/scenes/test.zs:12"));
    }

    @Test
    public void unregisteredBuildersBecomeDiagnosticIssues() {
        ScriptSceneRegistry.drainRegistrationIssues();
        ScriptSceneRegistry.create("minecraft:chest", "test:unregistered", "Unregistered",
            "ponder:demo/storage");

        ScriptSceneRegistry.reportUnregisteredBuilders();

        List<PonderDiagnosticIssue> issues = ScriptSceneRegistry.drainRegistrationIssues();
        assertEquals(1, issues.size());
        assertEquals("registration.script_unregistered", issues.get(0).getCode());
        assertEquals(new ResourceLocation("test", "unregistered"), issues.get(0).getSceneId());
    }

    @Test
    public void structureProviderFailuresAreIsolatedAsDiagnosticIssues() {
        ScriptSceneRegistry.drainRegistrationIssues();
        ResourceLocation sceneId = new ResourceLocation("test", "provider_failure");
        ScriptSceneRegistry.recordStructureFailure(sceneId,
            new ResourceLocation("test", "structure"), new IllegalStateException("fixture failure"));

        List<PonderDiagnosticIssue> issues = ScriptSceneRegistry.drainRegistrationIssues();
        assertEquals(1, issues.size());
        assertEquals("structure.provider_failed", issues.get(0).getCode());
        assertEquals(sceneId, issues.get(0).getSceneId());
        assertTrue(issues.get(0).getMessage().contains("fixture failure"));
    }
}
