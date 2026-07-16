package net.createmod.ponder.mmce.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import net.minecraft.util.ResourceLocation;

public class MMCEStructureRefTest {
    @Test
    public void staticReferenceRoundTripsAndDefaultsMachineNamespace() {
        MMCEStructureRef ref = MMCEStructureRef.unresolvedStatic("alloy_furnace", true)
            .resolved(repeat('a'), 5, 3, 4, 2, 1, 2);
        MMCEStructureRef parsed = MMCEStructureRef.tryParse(ref.asResourceLocation());

        assertNotNull(parsed);
        assertEquals("modularmachinery:alloy_furnace", parsed.machineId);
        assertFalse(parsed.dynamic);
        assertTrue(parsed.includePreviewNbt);
        assertEquals(64, parsed.fingerprint.length());
        assertEquals(ref.id, parsed.id);
        assertEquals("ponder_mmce:machine/modularmachinery/alloy_furnace", ref.component);
        assertEquals(5, ref.sizeX);
        assertEquals(5, ref.basePlateSize);
    }

    @Test
    public void dynamicReferenceContainsEveryExplicitParameter() {
        MMCEStructureRef ref = MMCEStructureRef.unresolvedDynamic(
            "modularmachinery:assembly_line", "heated middle", 4,
            "east", "south", false).resolved(repeat('b'), 9, 4, 3, 4, 1, 1);
        MMCEStructureRef parsed = MMCEStructureRef.tryParse(ref.asResourceLocation());

        assertNotNull(parsed);
        assertTrue(parsed.dynamic);
        assertEquals("heated middle", parsed.dynamicPattern);
        assertEquals(4, parsed.repetitions);
        assertEquals("east", parsed.patternOffset);
        assertEquals("south", parsed.facing);
        assertFalse(parsed.includePreviewNbt);
    }

    @Test
    public void structureIdChangesWithAnyStructureParameter() {
        MMCEStructureRef first = MMCEStructureRef.unresolvedDynamic(
            "modularmachinery:line", "middle", 2, "north", "north", true)
            .resolved(repeat('c'), 1, 1, 1, 0, 0, 0);
        MMCEStructureRef second = MMCEStructureRef.unresolvedDynamic(
            "modularmachinery:line", "middle", 3, "north", "north", true)
            .resolved(repeat('c'), 1, 1, 1, 0, 0, 0);

        assertNotEquals(first.id, second.id);
    }

    @Test
    public void tamperedStructureIdIsRejected() {
        MMCEStructureRef ref = MMCEStructureRef.unresolvedStatic("modularmachinery:test", true)
            .resolved(repeat('d'), 1, 1, 1, 0, 0, 0);
        String id = ref.id;
        ResourceLocation tampered = new ResourceLocation(id.substring(0, id.length() - 1) + "g");
        assertNull(MMCEStructureRef.tryParse(tampered));
    }

    @Test(expected = IllegalArgumentException.class)
    public void dynamicFacingMustBeHorizontal() {
        MMCEStructureRef.unresolvedDynamic(
            "modularmachinery:test", "middle", 1, "up", "north", true);
    }

    private static String repeat(char value) {
        StringBuilder result = new StringBuilder(64);
        for (int i = 0; i < 64; i++) result.append(value);
        return result.toString();
    }
}
