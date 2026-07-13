package net.createmod.ponder.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public class ScriptSceneDefinitionTest {
    @Test
    public void definitionRoundTripsThroughNbt() {
        NBTTagCompound idle = new NBTTagCompound();
        idle.setInteger("ticks", 640);
        ScriptSceneDefinition definition = new ScriptSceneDefinition(
            new ResourceLocation("minecraft", "paper"),
            new ResourceLocation("example", "paper"),
            "Paper",
            new ResourceLocation("example", "paper"),
            Arrays.asList(new ResourceLocation("example", "basics")),
            Arrays.asList(new ScriptInstruction("idle", idle), new ScriptInstruction("finish", null)),
            false);

        ScriptSceneDefinition decoded = ScriptSceneDefinition.deserialize(definition.serialize());
        assertEquals(definition.getComponent(), decoded.getComponent());
        assertEquals(definition.getSceneId(), decoded.getSceneId());
        assertEquals(definition.getStructure(), decoded.getStructure());
        assertEquals(2, decoded.getInstructions().size());
        assertFalse(decoded.isClientOnly());
    }

    @Test
    public void snapshotRoundTripsAndDetectsHash() throws Exception {
        NBTTagCompound idle = new NBTTagCompound();
        idle.setInteger("ticks", 20);
        ScriptSceneDefinition definition = new ScriptSceneDefinition(
            new ResourceLocation("minecraft", "paper"),
            new ResourceLocation("example", "paper"),
            "Paper",
            new ResourceLocation("example", "paper"),
            java.util.Collections.<ResourceLocation>emptyList(),
            Arrays.asList(new ScriptInstruction("idle", idle), new ScriptInstruction("finish", null)),
            false);
        ScriptSceneSnapshot.Encoded encoded = ScriptSceneSnapshot.encode(Arrays.asList(definition));
        List<ScriptSceneDefinition> decoded =
            ScriptSceneSnapshot.decode(encoded.bytes, encoded.uncompressedBytes);
        assertEquals(1, decoded.size());
        assertEquals(definition.getSceneId(), decoded.get(0).getSceneId());
        assertEquals(32, encoded.hash.length);
        assertTrue(encoded.bytes.length > 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderRejectsUnknownHandle() {
        ScriptSceneBuilder builder = new ScriptSceneBuilder("minecraft:paper", "example:paper", "Paper",
            "example:paper");
        builder.world.moveSection("missing", 1, 0, 0, 10);
    }

    @Test
    public void specialEntityInstructionsRoundTripThroughNbt() {
        ScriptSceneBuilder builder = new ScriptSceneBuilder("minecraft:paper", "example:entities", "Entities",
            "example:paper");
        builder.world.createMinecart("cart", 1.5, 1.1, 1.5, 15, "chest");
        builder.world.moveMinecart("cart", 0, 0, 2, 80);
        builder.world.rotateMinecart("cart", -90, 55);
        builder.world.createParrot("birb", 2.5, 2, 2.5, "face_point_of_interest");
        builder.world.changeParrotPose("birb", "dance");
        builder.world.moveParrot("birb", 0, 1, 0, 20);
        builder.world.rotateParrot("birb", 10, 20, 30, 20);
        builder.idle(20);
        builder.register();

        ScriptSceneDefinition registered = null;
        for (ScriptSceneDefinition candidate : ScriptSceneRegistry.localSnapshot(true))
            if (candidate.getSceneId().equals(new ResourceLocation("example", "entities"))) registered = candidate;
        assertTrue(registered != null);
        ScriptSceneDefinition decoded = ScriptSceneDefinition.deserialize(registered.serialize());
        assertEquals(8, decoded.getInstructions().size());
        assertEquals("create_minecart", decoded.getInstructions().get(0).getOperation());
        assertEquals("chest", decoded.getInstructions().get(0).getData().getString("type"));
        assertEquals("move_minecart", decoded.getInstructions().get(1).getOperation());
        assertEquals("rotate_minecart", decoded.getInstructions().get(2).getOperation());
        assertEquals("create_parrot", decoded.getInstructions().get(3).getOperation());
        assertEquals("face_poi", decoded.getInstructions().get(3).getData().getString("pose"));
        assertEquals("dance", decoded.getInstructions().get(4).getData().getString("pose"));
        assertEquals("rotate_parrot", decoded.getInstructions().get(6).getOperation());
        assertEquals(30D, decoded.getInstructions().get(6).getData().getDouble("z"), 0D);
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderRejectsMinecartHandleForParrotOperation() {
        ScriptSceneBuilder builder = new ScriptSceneBuilder("minecraft:paper", "example:entities_wrong",
            "Entities", "example:paper");
        builder.world.createMinecart("cart", 0, 0, 0, 0, "empty");
        builder.world.moveParrot("cart", 0, 1, 0, 20);
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderRejectsParrotHandleForSectionOperation() {
        ScriptSceneBuilder builder = new ScriptSceneBuilder("minecraft:paper", "example:entities_section_wrong",
            "Entities", "example:paper");
        builder.world.createParrot("birb", 0, 0, 0, "dance");
        builder.world.moveSection("birb", 0, 1, 0, 20);
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderRejectsUnknownMinecartType() {
        ScriptSceneBuilder builder = new ScriptSceneBuilder("minecraft:paper", "example:entities_type",
            "Entities", "example:paper");
        builder.world.createMinecart("cart", 0, 0, 0, 0, "modded");
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderRejectsUnknownParrotPose() {
        ScriptSceneBuilder builder = new ScriptSceneBuilder("minecraft:paper", "example:entities_pose",
            "Entities", "example:paper");
        builder.world.createParrot("birb", 0, 0, 0, "screech");
    }
}
