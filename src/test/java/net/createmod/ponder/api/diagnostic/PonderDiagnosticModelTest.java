package net.createmod.ponder.api.diagnostic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import net.minecraft.util.ResourceLocation;

public class PonderDiagnosticModelTest {
    @Test
    public void snapshotsAndEntriesAreImmutable() {
        ResourceLocation sceneId = new ResourceLocation("test", "scene");
        PonderDiagnosticIssue issue = new PonderDiagnosticIssue("structure.missing",
            PonderDiagnosticSeverity.ERROR, "missing", sceneId, -1);
        PonderSceneDiagnostic scene = new PonderSceneDiagnostic("test:entry", sceneId,
            new ResourceLocation("test", "component"), new ResourceLocation("test", "structure"),
            "Title", PonderSceneSource.LOCAL_ZS, "scripts/ponder/scenes/test.zs:1", "ponder",
            new ResourceLocation("ponder", "external_file"), "fingerprint",
            Collections.<ResourceLocation>emptyList(), 3, 20, Arrays.asList(5, 10),
            Collections.singletonList(issue), null);
        PonderDiagnosticSnapshot snapshot = new PonderDiagnosticSnapshot(PonderDiagnosticView.LOCAL,
            1, 2, Collections.singletonList(scene), Collections.singletonList(issue));
        assertNotNull(snapshot.findScene(sceneId));
        assertTrue(scene.hasErrors());
        assertEquals(2, scene.getKeyframes().size());
        try {
            snapshot.getScenes().clear();
            throw new AssertionError("Snapshot scenes must be immutable");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void overriddenCopyDoesNotMutateOriginal() {
        PonderSceneDiagnostic scene = new PonderSceneDiagnostic("test:entry",
            new ResourceLocation("test", "scene"), new ResourceLocation("test", "component"),
            new ResourceLocation("test", "structure"), "Title", PonderSceneSource.LOCAL_ZS,
            "", "ponder", null, "", Collections.<ResourceLocation>emptyList(), 1, 1,
            Collections.<Integer>emptyList(), Collections.<PonderDiagnosticIssue>emptyList(), null);
        assertTrue(!scene.isOverridden());
        assertEquals(PonderSceneSource.SERVER_SNAPSHOT,
            scene.overriddenBy(PonderSceneSource.SERVER_SNAPSHOT).getOverriddenBy());
    }
}
