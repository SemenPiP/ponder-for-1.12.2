package net.createmod.ponder.mmce.structure;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotEquals;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import net.createmod.ponder.mmce.script.MMCEStructureRef;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;

public class VanillaStructureEncoderTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void normalizesNegativeCoordinatesAndCopiesPreviewNbtAndGroups() throws Exception {
        MMCEStructureRef ref = MMCEStructureRef.unresolvedStatic("modularmachinery:test", true);
        NBTTagCompound preview = new NBTTagCompound();
        preview.setString("id", "minecraft:chest");
        preview.setString("CustomName", "Preview");

        List<VanillaStructureEncoder.SampledBlock> blocks = Arrays.asList(
            new VanillaStructureEncoder.SampledBlock(
                new BlockPos(-2, 0, 1), Blocks.STONE.getDefaultState(), null),
            new VanillaStructureEncoder.SampledBlock(
                new BlockPos(0, 1, -1), Blocks.CHEST.getDefaultState(), preview)
        );
        Map<String, List<BlockPos>> groups = new LinkedHashMap<String, List<BlockPos>>();
        groups.put("controller", Collections.singletonList(new BlockPos(0, 1, -1)));

        StructurePayload payload = VanillaStructureEncoder.encode(
            ref, blocks, groups, Collections.<String>emptyList());
        NBTTagCompound root = CompressedStreamTools.readCompressed(
            new ByteArrayInputStream(payload.getNbtBytes()));

        assertVector(root.getTagList("size", 3), 3, 2, 3);
        assertEquals(2, root.getTagList("blocks", 10).tagCount());
        NBTTagCompound chest = blockAt(root.getTagList("blocks", 10), new BlockPos(2, 1, 0));
        assertEquals("Preview", chest.getCompoundTag("nbt").getString("CustomName"));
        assertEquals(2, chest.getCompoundTag("nbt").getInteger("x"));
        assertEquals(1, chest.getCompoundTag("nbt").getInteger("y"));
        assertEquals(0, chest.getCompoundTag("nbt").getInteger("z"));
        assertEquals(Collections.singletonList(new BlockPos(2, 1, 0)),
            payload.getNamedGroups().get("controller"));
        assertTrue(payload.getFingerprint().matches("[0-9a-f]{64}"));
    }

    @Test
    public void encodingAndContentFingerprintAreDeterministic() throws Exception {
        MMCEStructureRef ref = MMCEStructureRef.unresolvedStatic("modularmachinery:test", false);
        List<VanillaStructureEncoder.SampledBlock> blocks = Arrays.asList(
            new VanillaStructureEncoder.SampledBlock(
                new BlockPos(1, 0, 0), Blocks.REDSTONE_WIRE.getDefaultState(), null),
            new VanillaStructureEncoder.SampledBlock(
                new BlockPos(0, 0, 0), Blocks.STONE.getDefaultState(), null)
        );

        StructurePayload first = VanillaStructureEncoder.encode(
            ref, blocks, Collections.<String, List<BlockPos>>emptyMap(),
            Collections.<String>emptyList());
        StructurePayload second = VanillaStructureEncoder.encode(
            ref, blocks, Collections.<String, List<BlockPos>>emptyMap(),
            Collections.<String>emptyList());

        assertEquals(first.getFingerprint(), second.getFingerprint());
        assertArrayEquals(first.getNbtBytes(), second.getNbtBytes());
    }

    @Test
    public void contentFingerprintIncludesNamedGroups() throws Exception {
        MMCEStructureRef ref = MMCEStructureRef.unresolvedStatic("modularmachinery:test", true);
        List<VanillaStructureEncoder.SampledBlock> blocks = Collections.singletonList(
            new VanillaStructureEncoder.SampledBlock(
                BlockPos.ORIGIN, Blocks.STONE.getDefaultState(), null));
        Map<String, List<BlockPos>> firstGroups = Collections.singletonMap(
            "mmce:tag/input", Collections.singletonList(BlockPos.ORIGIN));
        Map<String, List<BlockPos>> secondGroups = Collections.singletonMap(
            "mmce:tag/output", Collections.singletonList(BlockPos.ORIGIN));

        StructurePayload first = VanillaStructureEncoder.encode(
            ref, blocks, firstGroups, Collections.<String>emptyList());
        StructurePayload second = VanillaStructureEncoder.encode(
            ref, blocks, secondGroups, Collections.<String>emptyList());

        assertArrayEquals(first.getNbtBytes(), second.getNbtBytes());
        assertNotEquals(first.getFingerprint(), second.getFingerprint());
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

    private static void assertVector(NBTTagList list, int x, int y, int z) {
        assertEquals(x, list.getIntAt(0));
        assertEquals(y, list.getIntAt(1));
        assertEquals(z, list.getIntAt(2));
    }
}
