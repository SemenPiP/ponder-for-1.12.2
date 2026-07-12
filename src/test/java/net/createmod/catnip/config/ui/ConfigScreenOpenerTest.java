package net.createmod.catnip.config.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ConfigScreenOpenerTest {
    @Test
    public void resolvesModIdsAndFullConfigPaths() {
        assertNull(ConfigScreenOpener.resolveModId("  "));
        assertEquals("ponder", ConfigScreenOpener.resolveModId("ponder"));
        assertEquals("ponder", ConfigScreenOpener.resolveModId("ponder:client.client.editingMode"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidScreenTargets() {
        ConfigScreenOpener.resolveModId("ponder config");
    }
}
