package net.createmod.ponder.foundation.diagnostic;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class PonderDiagnosticServiceTest {
    @Test
    public void listDefaultsToEffectiveViewAndPaginates() {
        List<String> output = new ArrayList<String>();
        PonderDiagnosticService.execute("test", "list", output::add);
        assertTrue(output.get(0).contains("effective"));
        assertTrue(output.get(0).contains("page 1/1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownViews() {
        PonderDiagnosticService.execute("test", "list impossible", ignored -> {
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnsafeExportModeBeforeWriting() {
        PonderDiagnosticService.execute("test", "export test:missing executable effective", ignored -> {
        });
    }

    @Test
    public void dependenciesCommandWritesVersionedReport() {
        List<String> output = new ArrayList<String>();
        PonderDiagnosticService.execute("test", "dependencies effective", output::add);
        assertTrue(output.get(0).contains("structure dependencies"));
        assertTrue(output.get(0).contains("Report:"));
    }
}
