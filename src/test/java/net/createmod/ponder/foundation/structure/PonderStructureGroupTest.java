package net.createmod.ponder.foundation.structure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Arrays;
import java.util.Collections;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.api.structure.PonderStructureProviderResult;
import net.createmod.ponder.foundation.PonderSceneBuildingUtil;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

public class PonderStructureGroupTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @After
    public void clearResourceProvider() {
        PonderStructureLoader.setExternalRoot(null);
        PonderStructureLoader.setResourceProvider(null);
    }

    @Test
    public void embeddedAndBlockGroupsAreMergedDeterministically() throws Exception {
        NBTTagCompound root = structureRoot();
        PonderStructure structure = new PonderStructureLoader()
            .parse(root, new ResourceLocation("test", "groups"));

        assertEquals(Arrays.asList(new BlockPos(0, 0, 0), new BlockPos(1, 0, 0)),
            structure.getGroup("machine"));
        assertEquals(Collections.singletonList(new BlockPos(0, 0, 0)),
            structure.getGroup("input"));
        assertTrue(structure.getDiagnostics().toString().contains("outside declared size"));
        assertEquals("direct:test:groups", structure.getFingerprint());

        PonderLevel level = new PonderLevel(BlockPos.ORIGIN, null);
        structure.place(level);
        assertEquals(structure.getGroups(), level.getStructureGroups());
    }

    @Test
    public void sceneUtilityResolvesGroupsFromTheCurrentStructureOnly() throws Exception {
        PonderStructure structure = new PonderStructureLoader()
            .parse(structureRoot(), new ResourceLocation("test", "groups"));
        PonderSceneBuildingUtil util = new PonderSceneBuildingUtil(BlockPos.ORIGIN,
            new BlockPos(1, 0, 0), structure.getGroups());

        Selection machine = util.select().structureGroup("machine");
        assertEquals(2, machine.size());
        assertTrue(machine.test(new BlockPos(0, 0, 0)));
        assertTrue(machine.test(new BlockPos(1, 0, 0)));
        try {
            util.select().structureGroup("missing");
            fail("Missing structure groups must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("missing"));
        }
    }

    @Test
    public void publicLoadResultCarriesEmbeddedGroupsAndDiagnostics() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CompressedStreamTools.write(structureRoot(), new DataOutputStream(output));
        byte[] bytes = output.toByteArray();
        PonderStructureLoader.setResourceProvider(location -> new ByteArrayInputStream(bytes));

        PonderStructureProviderResult result = new PonderStructureLoader()
            .loadResult(new ResourceLocation("test", "groups-result"));
        assertTrue(result.isFound());
        assertEquals(Arrays.asList(new BlockPos(0, 0, 0), new BlockPos(1, 0, 0)),
            result.getGroups().get("machine"));
        assertTrue(result.getDiagnostics().toString().contains("outside declared size"));
        assertTrue(result.getNbtBytes().length > 0);
        assertEquals(64, result.getFingerprint().length());
    }

    private static NBTTagCompound structureRoot() {
        NBTTagCompound root = new NBTTagCompound();
        root.setTag("size", intVector(2, 1, 1));

        NBTTagList palette = new NBTTagList();
        NBTTagCompound stone = new NBTTagCompound();
        stone.setString("Name", "minecraft:stone");
        palette.appendTag(stone);
        root.setTag("palette", palette);

        NBTTagList blocks = new NBTTagList();
        blocks.appendTag(block(0, "input"));
        blocks.appendTag(block(1, null));
        root.setTag("blocks", blocks);
        root.setTag("entities", new NBTTagList());

        NBTTagList groups = new NBTTagList();
        NBTTagCompound machine = new NBTTagCompound();
        machine.setString("name", "machine");
        NBTTagList positions = new NBTTagList();
        positions.appendTag(intVector(0, 0, 0));
        positions.appendTag(intVector(1, 0, 0));
        positions.appendTag(intVector(1, 0, 0));
        positions.appendTag(intVector(4, 0, 0));
        machine.setTag("positions", positions);
        groups.appendTag(machine);
        root.setTag("ponder_groups", groups);
        return root;
    }

    private static NBTTagCompound block(int x, String group) {
        NBTTagCompound block = new NBTTagCompound();
        block.setTag("pos", intVector(x, 0, 0));
        block.setInteger("state", 0);
        if (group != null) {
            NBTTagList names = new NBTTagList();
            names.appendTag(new NBTTagString(group));
            block.setTag("ponder_groups", names);
        }
        return block;
    }

    private static NBTTagList intVector(int x, int y, int z) {
        NBTTagList list = new NBTTagList();
        list.appendTag(new NBTTagInt(x));
        list.appendTag(new NBTTagInt(y));
        list.appendTag(new NBTTagInt(z));
        return list;
    }
}
