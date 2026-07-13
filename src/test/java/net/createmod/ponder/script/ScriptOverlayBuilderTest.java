package net.createmod.ponder.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.List;

import org.junit.Test;

import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;

public class ScriptOverlayBuilderTest {
    @Test
    public void deterministicOverlayOperationsRoundTrip() {
        String sceneId = "test:overlay_" + Long.toHexString(System.nanoTime());
        ScriptSceneBuilder builder = new ScriptSceneBuilder("minecraft:paper", sceneId,
            "Overlay", "test:overlay");
        builder.overlay.showSharedText(20, "controls", new String[] {"one", "two"},
            ScriptVector.of(1, 2, 3), "blue", true, true);
        builder.overlay.showIndependentText(30, "Independent", 18, "white", false);
        builder.overlay.showOutlineWithText("Outlined", "green",
            ScriptSelection.position(1, 1, 1), 40, true);
        builder.overlay.showBoundingBox("red", "bounds", ScriptVector.of(0, 0, 0),
            ScriptVector.of(2, 3, 4), 50);
        builder.overlay.showScrollInput(ScriptVector.of(1.5, 2.5, 3.5), "up", 10);
        builder.overlay.showCenteredScrollInput(ScriptPosition.of(1, 2, 3), "north", 11);
        builder.overlay.showRepeaterScrollInput(ScriptPosition.of(2, 2, 2), 12);
        builder.overlay.showFilterSlotInput(ScriptVector.of(4.5, 5.5, 6.5), 13);
        builder.markAsFinished();
        builder.register();

        ScriptSceneDefinition definition = findScene(sceneId);
        ScriptSceneDefinition decoded = ScriptSceneDefinition.deserialize(definition.serialize());
        List<ScriptInstruction> instructions = decoded.getInstructions();
        assertEquals("show_shared_text", instructions.get(0).getOperation());
        assertEquals("test:controls", instructions.get(0).getData().getString("key"));
        NBTTagList params = instructions.get(0).getData().getTagList("params", 8);
        assertEquals(2, params.tagCount());
        assertEquals("show_independent_text", instructions.get(1).getOperation());
        assertEquals("show_outline_text", instructions.get(2).getOperation());
        assertEquals("show_bounding_box", instructions.get(3).getOperation());
        assertEquals("show_scroll_input", instructions.get(4).getOperation());
        assertEquals("show_centered_scroll_input", instructions.get(5).getOperation());
        assertEquals("show_repeater_scroll_input", instructions.get(6).getOperation());
        assertEquals("show_filter_slot_input", instructions.get(7).getOperation());
        assertFalse(decoded.isClientOnly());
    }

    private static ScriptSceneDefinition findScene(String sceneId) {
        ResourceLocation id = new ResourceLocation(sceneId);
        for (ScriptSceneDefinition definition : ScriptSceneRegistry.localSnapshot(true))
            if (definition.getSceneId().equals(id)) return definition;
        throw new AssertionError("Missing scene " + sceneId);
    }
}
