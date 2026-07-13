package net.createmod.ponder.foundation.instruction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.BeforeClass;
import org.junit.Test;

import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.PonderSceneBuilder;
import net.createmod.ponder.foundation.registration.PonderLocalization;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class ScriptEntityInstructionTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void linkedItemMovesWithSmoothDeterministicInterpolation() {
        PonderLevel world = world();
        PonderScene scene = scene(world, "move_item");
        PonderSceneBuilder builder = (PonderSceneBuilder) scene.builder();
        ElementLink<EntityElement> link = builder.world().createItemEntity(
            new Vec3d(1, 2, 3), Vec3d.ZERO, new ItemStack(Items.APPLE));
        builder.world().modifyEntity(link, entity -> entity.setNoGravity(true));
        builder.addInstruction(new AnimateEntityInstruction(link, new Vec3d(2, -1, 4), 10));
        builder.idle(10);

        scene.begin();
        scene.seek(10);

        EntityItem item = (EntityItem) world.getEntities().iterator().next();
        assertEquals(3, item.posX, 0);
        assertEquals(1, item.posY, 0);
        assertEquals(7, item.posZ, 0);

        scene.restart();
        scene.seek(10);
        EntityItem replayed = (EntityItem) world.getEntities().iterator().next();
        assertEquals(3, replayed.posX, 0);
        assertEquals(1, replayed.posY, 0);
        assertEquals(7, replayed.posZ, 0);
    }

    @Test
    public void visibilityAndRemovalOperateOnTheLinkedItemOnly() {
        PonderLevel world = world();
        PonderScene scene = scene(world, "remove_item");
        PonderSceneBuilder builder = (PonderSceneBuilder) scene.builder();
        ElementLink<EntityElement> link = builder.world().createItemEntity(
            new Vec3d(1, 2, 3), Vec3d.ZERO, new ItemStack(Items.APPLE));
        builder.addInstruction(EntityElementInstruction.setVisible(link, false));
        builder.idle(1);
        builder.addInstruction(EntityElementInstruction.setVisible(link, true));
        builder.idle(1);
        builder.addInstruction(EntityElementInstruction.remove(link));
        builder.idle(1);

        scene.begin();
        scene.tick();
        assertFalse(scene.resolve(link).isVisible());
        scene.tick();
        assertTrue(scene.resolve(link).isVisible());
        scene.tick();
        assertNull(scene.resolve(link));
        assertTrue(world.getEntities().isEmpty());
    }

    private static PonderLevel world() {
        PonderLevel world = new PonderLevel(BlockPos.ORIGIN, null);
        world.backup();
        return world;
    }

    private static PonderScene scene(PonderLevel world, String id) {
        return new PonderScene(world, new PonderLocalization(), "test",
            new ResourceLocation("test", id), Collections.emptyList(), Collections.emptyList());
    }
}
