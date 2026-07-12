package net.createmod.catnip.ghostblock;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;

public class GhostBlocksTest {
    private final GhostBlocks ghosts = GhostBlocks.getInstance();

    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @After
    public void clearGhosts() {
        ghosts.clear();
    }

    @Test
    public void ttlCountsCompleteFollowingTicksAndRefreshesExistingSlots() {
        Object slot = new Object();
        ghosts.showGhostState(slot, Blocks.STONE.getDefaultState(), 2);
        assertEquals(1, ghosts.size());

        ghosts.tickGhosts();
        assertEquals(1, ghosts.size());
        ghosts.showGhostState(slot, Blocks.GLASS.getDefaultState(), 2);
        ghosts.tickGhosts();
        assertEquals(1, ghosts.size());
        ghosts.tickGhosts();
        assertEquals(0, ghosts.size());
    }
}
