package net.createmod.ponder.script;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderSceneBuildingUtil;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

public class ScriptSelectionTest {
    @Test
    public void columnRoundTripsAndSpansSceneHeight() {
        ScriptSelection selection = ScriptSelection.column(4, 6);
        assertSerializedSelection("column", new int[] {4, 6}, selection);

        Selection resolved = ScriptSelection.deserialize(selection.serialize())
            .resolve(new PonderSceneBuildingUtil(new BlockPos(2, 3, 5), new BlockPos(8, 7, 11)));
        assertSelectionEquals(resolved,
            positions(4, 3, 6, 4, 7, 6));
    }

    @Test
    public void layersRoundTripsAndClampsToSceneBounds() {
        ScriptSelection selection = ScriptSelection.layers(2, 4);
        assertSerializedSelection("layers", new int[] {2, 4}, selection);

        Selection resolved = ScriptSelection.deserialize(selection.serialize())
            .resolve(new PonderSceneBuildingUtil(new BlockPos(1, 3, 9), new BlockPos(3, 6, 11)));
        assertSelectionEquals(resolved,
            positions(1, 3, 9, 3, 5, 11));
    }

    @Test(expected = IllegalArgumentException.class)
    public void layersRejectsNonPositiveHeight() {
        ScriptSelection.layers(4, 0);
    }

    @Test
    public void cuboidPreservesInclusiveOffsetsThroughNbtAndResolution() {
        ScriptSelection positive = ScriptSelection.cuboid(3, 4, 5, 2, 1, 0);
        assertSerializedSelection("cuboid", new int[] {3, 4, 5, 2, 1, 0}, positive);

        Selection positiveResolved = ScriptSelection.deserialize(positive.serialize())
            .resolve(new PonderSceneBuildingUtil(new BlockPos(0, 0, 0), new BlockPos(16, 16, 16)));
        assertSelectionEquals(positiveResolved,
            positions(3, 4, 5, 5, 5, 5));

        ScriptSelection negative = ScriptSelection.cuboid(6, 6, 6, -2, -1, -3);
        assertSerializedSelection("cuboid", new int[] {6, 6, 6, -2, -1, -3}, negative);

        Selection negativeResolved = ScriptSelection.deserialize(negative.serialize())
            .resolve(new PonderSceneBuildingUtil(new BlockPos(0, 0, 0), new BlockPos(16, 16, 16)));
        assertSelectionEquals(negativeResolved,
            positions(4, 5, 3, 6, 6, 6));
    }

    @Test
    public void existingSelectionFactoriesStillRoundTrip() {
        ScriptSelection[] selections = new ScriptSelection[] {
            ScriptSelection.position(1, 2, 3),
            ScriptSelection.fromTo(1, 2, 3, 4, 5, 6),
            ScriptSelection.layer(7),
            ScriptSelection.layersFrom(8),
            ScriptSelection.everywhere()
        };
        String[] types = new String[] {"position", "from_to", "layer", "layers_from", "everywhere"};
        int[][] values = new int[][] {
            new int[] {1, 2, 3},
            new int[] {1, 2, 3, 4, 5, 6},
            new int[] {7},
            new int[] {8},
            new int[0]
        };
        for (int i = 0; i < selections.length; i++) {
            ScriptSelection decoded = ScriptSelection.deserialize(selections[i].serialize());
            assertSerializedSelection(types[i], values[i], decoded);
        }
    }

    @Test
    public void structureGroupRoundTripsThroughTheCompatibleEnvelope() {
        ScriptSelection selection = ScriptSelection.structureGroup("machine/input");
        NBTTagCompound serialized = selection.serialize();
        assertEquals("everywhere", serialized.getString("type"));
        assertEquals("machine/input", serialized.getString("structure_group"));
        assertArrayEquals(new int[0], serialized.getIntArray("values"));

        Map<String, Collection<BlockPos>> groups =
            new LinkedHashMap<String, Collection<BlockPos>>();
        groups.put("machine/input", Arrays.asList(new BlockPos(1, 2, 3), new BlockPos(4, 5, 6)));
        Selection resolved = ScriptSelection.deserialize(serialized)
            .resolve(new PonderSceneBuildingUtil(BlockPos.ORIGIN, new BlockPos(8, 8, 8), groups));
        assertSelectionEquals(resolved, new LinkedHashSet<BlockPos>(
            Arrays.asList(new BlockPos(1, 2, 3), new BlockPos(4, 5, 6))));

        NBTTagCompound instructionData = new NBTTagCompound();
        instructionData.setTag("selection", serialized);
        instructionData.setString("direction", "up");
        ScriptInstructionValidator.validate(new net.minecraft.util.ResourceLocation("test", "group"),
            Collections.singletonList(new ScriptInstruction("show_section", instructionData)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingStructureGroupIsRejected() {
        ScriptSelection.deserialize(
            ScriptSelection.structureGroup("missing").serialize())
            .resolve(new PonderSceneBuildingUtil(BlockPos.ORIGIN, new BlockPos(2, 2, 2)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void resolveRejectsUnknownSelectionType() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("type", "unknown");
        tag.setIntArray("values", new int[0]);
        ScriptSelection.deserialize(tag).resolve(new PonderSceneBuildingUtil(new BlockPos(0, 0, 0),
            new BlockPos(0, 0, 0)));
    }

    private static void assertSerializedSelection(String expectedType, int[] expectedValues,
                                                  ScriptSelection selection) {
        NBTTagCompound serialized = selection.serialize();
        assertEquals(expectedType, serialized.getString("type"));
        assertArrayEquals(expectedValues, serialized.getIntArray("values"));
        ScriptSelection decoded = ScriptSelection.deserialize(serialized);
        NBTTagCompound reserialized = decoded.serialize();
        assertEquals(expectedType, reserialized.getString("type"));
        assertArrayEquals(expectedValues, reserialized.getIntArray("values"));
    }

    private static void assertSelectionEquals(Selection selection, Set<BlockPos> expectedPositions) {
        Set<BlockPos> actual = new LinkedHashSet<BlockPos>();
        for (BlockPos pos : selection) actual.add(pos);
        assertEquals(expectedPositions, actual);
    }

    private static Set<BlockPos> positions(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        Set<BlockPos> result = new LinkedHashSet<BlockPos>();
        for (int y = minY; y <= maxY; y++)
            for (int z = minZ; z <= maxZ; z++)
                for (int x = minX; x <= maxX; x++)
                    result.add(new BlockPos(x, y, z));
        return result;
    }
}
