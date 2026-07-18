package net.createmod.ponder.mmce.structure;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import net.minecraft.util.math.BlockPos;

public class MMCEDynamicExpansionTest {
    @Test
    public void calculatesEveryRepeatedSegmentOffset() {
        assertEquals(Arrays.asList(
                new BlockPos(0, 0, 1),
                new BlockPos(0, 0, 2),
                new BlockPos(0, 0, 3)),
            MMCEStructureProvider.dynamicOffsets(
                3, new BlockPos(0, 0, 1), new BlockPos(0, 0, 1)));
    }

    @Test
    public void matchesMmceDynamicTagNaming() {
        assertEquals("frame_lane_0",
            MMCEStructureProvider.dynamicTag("frame", "lane", "0"));
        assertEquals("output_lane_end",
            MMCEStructureProvider.dynamicTag("output", "lane", "end"));
    }
}
