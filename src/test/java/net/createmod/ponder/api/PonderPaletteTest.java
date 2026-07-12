package net.createmod.ponder.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class PonderPaletteTest {
    @Test
    public void colorsAreOpaqueArgbAndStable() {
        assertEquals(0xff7fcde0, PonderPalette.INPUT.getColor());
        assertEquals(255, PonderPalette.INPUT.getColorObject().getAlpha());
        assertSame(PonderPalette.INPUT.getColorObject(), PonderPalette.INPUT.getColorObject());
    }
}
