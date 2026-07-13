package net.createmod.ponder.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public class ScriptSceneBuilderOverloadTest {
    @Test
    public void overloadsPopulateInstructionsAndKeepSourceOutOfSerialization() {
        ScriptSceneBuilder builder = new ScriptSceneBuilder("minecraft:paper", "example:overloads", "Overloads",
            "example:paper", "D:/scripts/overloads.zs:12");

        builder.effects.indicateRedstone(ScriptPosition.of(1, 2, 3));
        builder.effects.indicateSuccess(ScriptPosition.of(4, 5, 6));
        builder.effects.createRedstoneParticles(ScriptPosition.of(7, 8, 9), 0xFF0000, 12);
        builder.effects.emitParticles("flame", ScriptVector.of(1.5, 2.5, 3.5),
            ScriptVector.of(0.1, 0.2, 0.3), 4.5F, 20);
        builder.effects.emitParticlesWithinBlock("smoke_normal", ScriptVector.of(4.5, 5.5, 6.5),
            ScriptVector.of(0.0, 0.1, 0.0), 1.0F, 40);
        builder.movePointOfInterest(ScriptVector.of(10.5, 11.5, 12.5));
        builder.register();

        ScriptSceneDefinition definition = findScene("example:overloads");
        NBTTagCompound serialized = definition.serialize();
        assertFalse(serialized.hasKey("source"));
        assertEquals(6, definition.getInstructions().size());
        assertEquals("indicate_redstone", definition.getInstructions().get(0).getOperation());
        assertEquals(1, definition.getInstructions().get(0).getData().getInteger("x"));
        assertEquals("indicate_success", definition.getInstructions().get(1).getOperation());
        assertEquals("redstone_particles", definition.getInstructions().get(2).getOperation());
        assertEquals("particles", definition.getInstructions().get(3).getOperation());
        assertEquals(10.5D, definition.getInstructions().get(5).getData().getDouble("x"), 0D);
    }

    @Test
    public void builderCreationFailuresIncludeSource() {
        try {
            new ScriptSceneBuilder("", "example:bad", "Bad", "example:paper",
                "D:/scripts/bad.zs:7");
            fail("Invalid builder ids must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("D:/scripts/bad.zs:7"));
            assertTrue(expected.getMessage().contains("component id"));
        }
    }

    @Test
    public void defaultConstructorIsSafeWithoutCraftTweaker() {
        ScriptSceneBuilder builder = new ScriptSceneBuilder("minecraft:paper", "example:capture_source",
            "Capture Source", "example:paper");
        builder.idle(1);
    }

    @Test
    public void registerFailuresIncludeSource() {
        String sceneId = "example:duplicate_" + Long.toHexString(System.nanoTime());
        ScriptSceneBuilder first = new ScriptSceneBuilder("minecraft:paper", sceneId, "First", "example:paper",
            "D:/scripts/first.zs:14");
        first.idle(1);
        first.register();

        ScriptSceneBuilder second = new ScriptSceneBuilder("minecraft:paper", sceneId, "Second", "example:paper",
            "D:/scripts/second.zs:29");
        second.idle(1);
        try {
            second.register();
            fail("Duplicate scene ids must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("D:/scripts/second.zs:29"));
            assertTrue(expected.getMessage().contains("Duplicate Ponder script scene id"));
        }
    }

    @Test
    public void vectorsRejectNonFiniteComponents() {
        try {
            ScriptVector.of(Double.NaN, 0, 0);
            fail("Non-finite vectors must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("finite"));
        }
    }

    private static ScriptSceneDefinition findScene(String sceneId) {
        ResourceLocation id = new ResourceLocation(sceneId);
        List<ScriptSceneDefinition> definitions = ScriptSceneRegistry.localSnapshot(true);
        for (ScriptSceneDefinition definition : definitions)
            if (definition.getSceneId().equals(id)) return definition;
        throw new AssertionError("Missing scene " + sceneId);
    }
}
