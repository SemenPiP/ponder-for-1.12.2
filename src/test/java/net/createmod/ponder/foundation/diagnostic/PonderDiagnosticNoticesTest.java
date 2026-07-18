package net.createmod.ponder.foundation.diagnostic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import net.createmod.ponder.api.diagnostic.PonderDiagnosticIssue;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticSeverity;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticView;
import net.minecraft.util.ResourceLocation;

public class PonderDiagnosticNoticesTest {
    @Test
    public void summariesAreDeduplicatedWithinOneGeneration() {
        PonderDiagnosticNotices.beginGeneration(1001);
        PonderDiagnosticNotices.record(new PonderDiagnosticIssue("structure.missing",
            PonderDiagnosticSeverity.ERROR, "missing"));
        PonderDiagnosticNotices.record(new PonderDiagnosticIssue("structure.missing",
            PonderDiagnosticSeverity.ERROR, "missing"));
        PonderDiagnosticNotices.record(new PonderDiagnosticIssue("structure.warning",
            PonderDiagnosticSeverity.WARNING, "warning"));

        String summary = PonderDiagnosticNotices.drainSummary();
        assertTrue(summary.contains("2 diagnostic issue(s)"));
        assertTrue(summary.contains("1 error(s)"));
        assertTrue(summary.contains("1 warning(s)"));
        assertNull(PonderDiagnosticNotices.drainSummary());

        PonderDiagnosticNotices.record(new PonderDiagnosticIssue("structure.missing",
            PonderDiagnosticSeverity.ERROR, "missing"));
        assertNull(PonderDiagnosticNotices.drainSummary());
    }

    @Test
    public void newGenerationAllowsARepeatedIssueSummary() {
        PonderDiagnosticNotices.beginGeneration(1002);
        PonderDiagnosticIssue issue = new PonderDiagnosticIssue("sync.rejected",
            PonderDiagnosticSeverity.ERROR, "rejected");
        PonderDiagnosticNotices.record(issue);
        assertTrue(PonderDiagnosticNotices.drainSummary().contains("1 diagnostic issue(s)"));

        PonderDiagnosticNotices.beginGeneration(1003);
        PonderDiagnosticNotices.record(issue);
        assertTrue(PonderDiagnosticNotices.drainSummary().contains("1 diagnostic issue(s)"));
    }

    @Test
    public void runtimeIssuesEnterEveryViewWithoutDuplicates() {
        PonderDiagnosticNotices.beginGeneration(1004);
        ResourceLocation sceneId = new ResourceLocation("test", "runtime");
        PonderDiagnosticIssue issue = new PonderDiagnosticIssue("sync.rejected",
            PonderDiagnosticSeverity.ERROR, "runtime failure", sceneId, -1);

        int localBefore = PonderDiagnosticRegistry.snapshot(PonderDiagnosticView.LOCAL).getIssues().size();
        int serverBefore = PonderDiagnosticRegistry.snapshot(PonderDiagnosticView.SERVER).getIssues().size();
        int effectiveBefore = PonderDiagnosticRegistry.snapshot(PonderDiagnosticView.EFFECTIVE).getIssues().size();
        PonderDiagnosticRegistry.recordRuntimeIssue(issue);
        PonderDiagnosticRegistry.recordRuntimeIssue(issue);

        assertEquals(localBefore + 1,
            PonderDiagnosticRegistry.snapshot(PonderDiagnosticView.LOCAL).getIssues().size());
        assertEquals(serverBefore + 1,
            PonderDiagnosticRegistry.snapshot(PonderDiagnosticView.SERVER).getIssues().size());
        assertEquals(effectiveBefore + 1,
            PonderDiagnosticRegistry.snapshot(PonderDiagnosticView.EFFECTIVE).getIssues().size());
    }
}
