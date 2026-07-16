package net.createmod.ponder.mmce.structure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import hellfirepvp.modularmachinery.common.crafting.helper.ComponentSelectorTag;
import hellfirepvp.modularmachinery.common.machine.TaggedPositionBlockArray;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import net.createmod.ponder.mmce.script.MMCEStructureRef;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;

public class MMCEBlockArrayAdapterTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void usesOriginalPackedPositionForSamplingAndExportsPreviewAndTag() throws Exception {
        TaggedPositionBlockArray array = mock(TaggedPositionBlockArray.class);
        BlockPos sampledPosition = new BlockPos(31, 0, 0);
        BlockArray.BlockInformation cycling = mock(BlockArray.BlockInformation.class);
        IBlockState expected = Blocks.DIRT.getDefaultState();
        when(cycling.getSampleState(sampledPosition.toLong())).thenReturn(expected);
        when(cycling.getPreviewTag()).thenReturn(null);

        BlockPos negativePosition = new BlockPos(-2, -1, -3);
        BlockArray.BlockInformation controller = mock(BlockArray.BlockInformation.class);
        NBTTagCompound preview = new NBTTagCompound();
        preview.setString("id", "minecraft:chest");
        preview.setString("CustomName", "MMCE Preview");
        when(controller.getSampleState(negativePosition.toLong()))
            .thenReturn(Blocks.CHEST.getDefaultState());
        when(controller.getPreviewTag()).thenReturn(preview);

        Map<BlockPos, BlockArray.BlockInformation> pattern =
            new LinkedHashMap<BlockPos, BlockArray.BlockInformation>();
        pattern.put(sampledPosition, cycling);
        pattern.put(negativePosition, controller);
        when(array.getPattern()).thenReturn(pattern);
        when(array.getTaggedPositions()).thenReturn(Collections.singletonMap(
            negativePosition, new ComponentSelectorTag("controller")));

        MMCEStructureRef ref = MMCEStructureRef.unresolvedStatic("modularmachinery:test", true);
        StructurePayload payload = new MMCEBlockArrayAdapter().convert(
            ref, array, Collections.<String>emptyList());
        NBTTagCompound root = CompressedStreamTools.readCompressed(
            new ByteArrayInputStream(payload.getNbtBytes()));

        BlockPos sampledNormalized = sampledPosition.subtract(negativePosition);
        assertEquals(expected.getBlock().getRegistryName().toString(),
            stateNameAt(root, sampledNormalized));
        NBTTagCompound controllerBlock = blockAt(root.getTagList("blocks", 10), BlockPos.ORIGIN);
        assertEquals("MMCE Preview", controllerBlock.getCompoundTag("nbt").getString("CustomName"));
        assertEquals(Collections.singletonList(BlockPos.ORIGIN),
            payload.getNamedGroups().get("mmce:tag/controller"));
        assertEquals(Collections.singletonList(new BlockPos(2, 1, 3)),
            payload.getNamedGroups().get("mmce:controller"));
        assertEquals(2, payload.getNamedGroups().get("mmce:all").size());
        assertTrue(payload.getDiagnostics().isEmpty());
        verify(cycling).getSampleState(sampledPosition.toLong());
        verify(controller).getSampleState(negativePosition.toLong());
    }

    @Test(expected = IOException.class)
    public void failedSampleRejectsTheEntireStructure() throws Exception {
        TaggedPositionBlockArray array = mock(TaggedPositionBlockArray.class);
        BlockPos position = new BlockPos(1, 2, 3);
        BlockArray.BlockInformation information = mock(BlockArray.BlockInformation.class);
        when(information.getSampleState(position.toLong()))
            .thenThrow(new IllegalStateException("no deterministic sample"));
        when(array.getPattern()).thenReturn(Collections.singletonMap(position, information));
        when(array.getTaggedPositions()).thenReturn(Collections.emptyMap());

        new MMCEBlockArrayAdapter().convert(
            MMCEStructureRef.unresolvedStatic("modularmachinery:test", true),
            array, Collections.<String>emptyList());
    }

    private static String stateNameAt(NBTTagCompound root, BlockPos target) {
        NBTTagCompound block = blockAt(root.getTagList("blocks", 10), target);
        int state = block.getInteger("state");
        return root.getTagList("palette", 10).getCompoundTagAt(state).getString("Name");
    }

    private static NBTTagCompound blockAt(NBTTagList blocks, BlockPos target) {
        for (int i = 0; i < blocks.tagCount(); i++) {
            NBTTagCompound block = blocks.getCompoundTagAt(i);
            NBTTagList pos = block.getTagList("pos", 3);
            if (pos.getIntAt(0) == target.getX()
                && pos.getIntAt(1) == target.getY()
                && pos.getIntAt(2) == target.getZ())
                return block;
        }
        throw new AssertionError("Missing block at " + target);
    }
}
