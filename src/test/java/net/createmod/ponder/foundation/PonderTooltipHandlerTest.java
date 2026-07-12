package net.createmod.ponder.foundation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PonderTooltipHandlerTest {
    @Test
    public void heldInputAdvancesAndClampsProgress() {
        assertEquals(.09f, PonderTooltipHandler.advanceProgress(0, true), .0001f);
        assertEquals(1, PonderTooltipHandler.advanceProgress(.96f, true), .0001f);
    }

    @Test
    public void releasedInputDecaysAndClampsProgress() {
        assertEquals(.38f, PonderTooltipHandler.advanceProgress(.5f, false), .0001f);
        assertEquals(0, PonderTooltipHandler.advanceProgress(.05f, false), .0001f);
    }

    @Test
    public void progressBarAlwaysHasTenSegments() {
        String bar = PonderTooltipHandler.progressBar(.5f);
        assertEquals(10, bar.length() - bar.replace("|", "").length());
        assertTrue(bar.endsWith("]"));
    }
}
