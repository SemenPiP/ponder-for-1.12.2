package net.createmod.ponder.foundation.diagnostic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;

import com.google.gson.JsonObject;

import net.createmod.ponder.api.diagnostic.PonderDiagnosticSnapshot;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticIssue;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticSeverity;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticView;
import net.createmod.ponder.api.diagnostic.PonderSceneDiagnostic;
import net.createmod.ponder.api.diagnostic.PonderSceneSource;
import net.minecraft.util.ResourceLocation;

public class PonderDiagnosticReportsTest {
    @Test
    public void reportContainsStableFormatAndSanitizedSource() {
        PonderDiagnosticIssue issue = new PonderDiagnosticIssue("structure.warning",
            PonderDiagnosticSeverity.WARNING, "warning", new ResourceLocation("test", "scene"), 2);
        PonderSceneDiagnostic scene = new PonderSceneDiagnostic("test:entry",
            new ResourceLocation("test", "scene"), new ResourceLocation("test", "component"),
            new ResourceLocation("test", "structure"), "Title", PonderSceneSource.LOCAL_ZS,
            "scripts/ponder/scenes/test.zs:4", "ponder", null, "",
            Collections.singletonList(new ResourceLocation("test", "tag")), 1, 20,
            Collections.<Integer>emptyList(), Collections.singletonList(issue),
            PonderSceneSource.SERVER_SNAPSHOT);
        PonderDiagnosticSnapshot snapshot = new PonderDiagnosticSnapshot(PonderDiagnosticView.LOCAL,
            3, 4, Collections.singletonList(scene), Collections.singletonList(issue));
        JsonObject json = PonderDiagnosticReports.snapshotJson(snapshot);
        assertEquals(1, json.get("format").getAsInt());
        String encoded = json.toString();
        assertFalse(encoded.contains("C:\\\\"));
        assertFalse(encoded.contains("/home/"));
        assertTrue(encoded.contains("\"overriddenBy\":\"SERVER_SNAPSHOT\""));
        assertTrue(encoded.contains("\"tags\":[\"test:tag\"]"));
        assertTrue(encoded.contains("\"instructionIndex\":2"));
    }
}
