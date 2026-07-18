package net.createmod.ponder.foundation.diagnostic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Collections;

import org.junit.Test;

import com.google.gson.JsonObject;

import net.createmod.ponder.api.diagnostic.PonderDiagnosticSnapshot;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticView;
import net.createmod.ponder.api.diagnostic.PonderSceneDiagnostic;
import net.createmod.ponder.api.diagnostic.PonderSceneSource;
import net.minecraft.util.ResourceLocation;

public class PonderDiagnosticReportsTest {
    @Test
    public void reportContainsStableFormatAndSanitizedSource() {
        PonderSceneDiagnostic scene = new PonderSceneDiagnostic("test:entry",
            new ResourceLocation("test", "scene"), new ResourceLocation("test", "component"),
            new ResourceLocation("test", "structure"), "Title", PonderSceneSource.LOCAL_ZS,
            "scripts/ponder/scenes/test.zs:4", "ponder", null, "",
            Collections.<ResourceLocation>emptyList(), 1, 20,
            Collections.<Integer>emptyList(), Collections.emptyList(), null);
        PonderDiagnosticSnapshot snapshot = new PonderDiagnosticSnapshot(PonderDiagnosticView.LOCAL,
            3, 4, Collections.singletonList(scene), Collections.emptyList());
        JsonObject json = PonderDiagnosticReports.snapshotJson(snapshot);
        assertEquals(1, json.get("format").getAsInt());
        String encoded = json.toString();
        assertFalse(encoded.contains("C:\\\\"));
        assertFalse(encoded.contains("/home/"));
    }
}
