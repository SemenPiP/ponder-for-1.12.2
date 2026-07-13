package net.createmod.ponder.script;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public class ScriptInstructionValidatorTest {
    private static final ResourceLocation SCENE = new ResourceLocation("test", "validator");

    @Test
    public void rejectsNonFiniteVectorAfterNetworkDecode() {
        NBTTagCompound data = new NBTTagCompound();
        data.setDouble("x", Double.NaN);
        data.setDouble("y", 0);
        data.setDouble("z", 0);
        data.setString("handle", "section");
        data.setInteger("duration", 10);
        assertInvalid(Arrays.asList(
            instruction("show_independent_immediate", selectionWithHandle("section")),
            instruction("move_section", data)),
            "must be finite");
    }

    @Test
    public void rejectsWrongHandleTypeAfterNetworkDecode() {
        NBTTagCompound item = ScriptWorldBuilder.vector(0, 0, 0);
        item.setString("handle", "item");
        item.setDouble("mx", 0);
        item.setDouble("my", 0);
        item.setDouble("mz", 0);
        item.setString("item", "minecraft:apple");
        item.setInteger("count", 1);
        item.setInteger("meta", 0);

        NBTTagCompound move = ScriptWorldBuilder.vector(1, 0, 0);
        move.setString("handle", "item");
        move.setInteger("duration", 10);
        assertInvalid(Arrays.asList(instruction("create_item", item), instruction("move_section", move)),
            "expected section");
    }

    @Test
    public void rejectsItemUseAfterRemoval() {
        NBTTagCompound item = ScriptWorldBuilder.vector(0, 0, 0);
        item.setString("handle", "item");
        item.setDouble("mx", 0);
        item.setDouble("my", 0);
        item.setDouble("mz", 0);
        item.setString("item", "minecraft:apple");
        item.setInteger("count", 1);
        item.setInteger("meta", 0);
        NBTTagCompound remove = new NBTTagCompound();
        remove.setString("handle", "item");
        NBTTagCompound visible = new NBTTagCompound();
        visible.setString("handle", "item");
        visible.setBoolean("visible", true);
        assertInvalid(Arrays.asList(instruction("create_item", item), instruction("remove_item", remove),
            instruction("set_item_visible", visible)), "terminated");
    }

    @Test
    public void acceptsValidItemLifecycle() {
        ScriptSceneBuilder builder = new ScriptSceneBuilder("minecraft:paper", "test:item_lifecycle",
            "Item lifecycle", "test:item");
        builder.world.createItemEntity("item", 0, 1, 0, 0, 0, 0, "minecraft:apple", 1, 0);
        builder.world.moveItem("item", 1, 0, 0, 10);
        builder.world.hideItem("item");
        builder.world.showItem("item");
        builder.world.removeItem("item");
        builder.markAsFinished();
        builder.register();
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderRejectsReusingRemovedItemHandle() {
        ScriptSceneBuilder builder = new ScriptSceneBuilder("minecraft:paper", "test:item_reuse",
            "Item reuse", "test:item");
        builder.world.createItemEntity("item", 0, 1, 0, 0, 0, 0, "minecraft:apple", 1, 0);
        builder.world.removeItem("item");
        builder.world.createItemEntity("item", 1, 1, 0, 0, 0, 0, "minecraft:apple", 1, 0);
    }

    private static NBTTagCompound selectionWithHandle(String handle) {
        NBTTagCompound data = new NBTTagCompound();
        data.setString("handle", handle);
        data.setTag("selection", ScriptSelection.position(0, 0, 0).serialize());
        return data;
    }

    private static ScriptInstruction instruction(String operation, NBTTagCompound data) {
        return new ScriptInstruction(operation, data);
    }

    private static void assertInvalid(java.util.List<ScriptInstruction> instructions, String message) {
        try {
            ScriptInstructionValidator.validate(SCENE, instructions);
            fail("Expected invalid instruction sequence");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains(message));
        }
    }
}
