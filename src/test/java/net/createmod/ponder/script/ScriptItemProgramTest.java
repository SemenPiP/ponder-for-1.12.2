package net.createmod.ponder.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.BeforeClass;
import org.junit.Test;

import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.registration.PonderLocalization;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

public class ScriptItemProgramTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void scriptProgramKeepsAndTerminatesItemHandles() {
        String sceneId = "test:item_program_" + Long.toHexString(System.nanoTime());
        ScriptSceneBuilder script = new ScriptSceneBuilder("minecraft:paper", sceneId,
            "Item program", "test:item");
        script.world.createItemEntity("item", 1, 2, 3, 0, 0, 0, "minecraft:apple", 1, 0);
        script.world.moveItem("item", ScriptVector.of(2, -1, 4), 5);
        script.idle(5);
        script.world.hideItem("item");
        script.idle(1);
        script.world.showItem("item");
        script.idle(1);
        script.world.removeItem("item");
        script.idle(1);
        script.markAsFinished();
        script.register();

        ScriptSceneDefinition definition = findScene(sceneId);
        PonderLevel world = new PonderLevel(BlockPos.ORIGIN, null);
        world.backup();
        PonderScene scene = new PonderScene(world, new PonderLocalization(), "test",
            new ResourceLocation("minecraft", "paper"), Collections.emptyList(), Collections.emptyList());
        definition.asStoryBoard().program(scene.builder(), scene.getSceneBuildingUtil());
        scene.begin();
        scene.seek(5);

        EntityItem item = (EntityItem) world.getEntities().iterator().next();
        assertEquals(3, item.posX, 0);
        assertEquals(1, item.posY, 0);
        assertEquals(7, item.posZ, 0);

        scene.tick();
        assertFalse(entityElement(scene).isVisible());
        scene.tick();
        assertTrue(entityElement(scene).isVisible());
        scene.tick();
        assertTrue(world.getEntities().isEmpty());
        final int[] remaining = new int[1];
        scene.forEach(EntityElement.class, element -> remaining[0]++);
        assertEquals(0, remaining[0]);
    }

    private static EntityElement entityElement(PonderScene scene) {
        final EntityElement[] result = new EntityElement[1];
        scene.forEach(EntityElement.class, element -> result[0] = element);
        if (result[0] == null) throw new AssertionError("Missing entity element");
        return result[0];
    }

    private static ScriptSceneDefinition findScene(String sceneId) {
        ResourceLocation id = new ResourceLocation(sceneId);
        for (ScriptSceneDefinition definition : ScriptSceneRegistry.localSnapshot(true))
            if (definition.getSceneId().equals(id)) return definition;
        throw new AssertionError("Missing scene " + sceneId);
    }
}
