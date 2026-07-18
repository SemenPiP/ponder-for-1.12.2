package net.createmod.ponder.foundation.diagnostic;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import net.createmod.ponder.api.diagnostic.PonderDiagnosticIssue;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticSeverity;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticSnapshot;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticView;
import net.createmod.ponder.api.diagnostic.PonderSceneDiagnostic;
import net.createmod.ponder.api.diagnostic.PonderSceneSource;
import net.minecraft.util.ResourceLocation;

public class PonderValidationManagerTest {
    @Test
    public void processesOneScenePerTickAndSeparatesSeverityCounts() {
        PonderDiagnosticIssue error = new PonderDiagnosticIssue("structure.missing",
            PonderDiagnosticSeverity.ERROR, "missing", new ResourceLocation("test", "first"), -1);
        PonderDiagnosticIssue warning = new PonderDiagnosticIssue("structure.warning",
            PonderDiagnosticSeverity.WARNING, "warning", new ResourceLocation("test", "second"), -1);
        PonderDiagnosticIssue globalError = new PonderDiagnosticIssue("sync.rejected",
            PonderDiagnosticSeverity.ERROR, "rejected");
        PonderSceneDiagnostic first = scene("first", Collections.singletonList(error));
        PonderSceneDiagnostic second = scene("second", Collections.singletonList(warning));
        PonderDiagnosticSnapshot snapshot = new PonderDiagnosticSnapshot(PonderDiagnosticView.EFFECTIVE,
            1, 2, Arrays.asList(first, second), Arrays.asList(error, warning, globalError));
        List<String> output = new ArrayList<String>();
        String side = "validation-test";

        assertTrue(PonderValidationManager.start(side, snapshot, output::add));
        PonderValidationManager.tick(side);
        assertTrue(PonderValidationManager.isRunning(side));
        PonderValidationManager.tick(side);
        assertFalse(PonderValidationManager.isRunning(side));
        assertTrue(output.get(output.size() - 1).contains("2 error(s), 1 warning(s)"));
    }

    private static PonderSceneDiagnostic scene(String path, List<PonderDiagnosticIssue> issues) {
        ResourceLocation id = new ResourceLocation("test", path);
        return new PonderSceneDiagnostic("test:" + path, id,
            new ResourceLocation("test", "component"), new ResourceLocation("test", "structure"),
            path, PonderSceneSource.LOCAL_ZS, "scripts/ponder/scenes/test.zs:1", "ponder",
            null, "", Collections.<ResourceLocation>emptyList(), 1, 20,
            Collections.<Integer>emptyList(), issues, null);
    }
}
