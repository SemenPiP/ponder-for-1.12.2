package net.createmod.ponder.mmce.structure;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import hellfirepvp.modularmachinery.common.machine.TaggedPositionBlockArray;
import net.createmod.ponder.mmce.script.MMCEStructureRef;
import net.minecraft.util.EnumFacing;

public class MMCEDynamicExpansionTest {
    @Test
    public void expandsOnAStaticCopyThroughTheMmceDynamicPatternApi() {
        TaggedPositionBlockArray base = mock(TaggedPositionBlockArray.class);
        TaggedPositionBlockArray expanded = mock(TaggedPositionBlockArray.class);
        MMCEStructureRef ref = MMCEStructureRef.unresolvedDynamic(
            "modularmachinery:test", "lane", 2, "east", "south", false);

        TaggedPositionBlockArray result = MMCEStructureProvider.expandDynamic(
            base, ref, source -> {
                assertSame(base, source);
                return expanded;
            }, (target, repetitions, dynamicFacing, machineFacing) -> {
                assertSame(expanded, target);
                org.junit.Assert.assertEquals(2, repetitions);
                org.junit.Assert.assertEquals(EnumFacing.EAST, dynamicFacing);
                org.junit.Assert.assertEquals(EnumFacing.SOUTH, machineFacing);
            });

        assertSame(expanded, result);
    }
}
