package net.createmod.ponder.foundation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.BeforeClass;
import org.junit.Test;

import net.createmod.catnip.data.Pair;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.foundation.registration.PonderLocalization;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class PonderSceneRayTraceTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void nearestVisibleSectionWinsAndMissClearsTheResult() {
        PonderWorld world = new PonderWorld(BlockPos.ORIGIN, null);
        BlockPos nearPos = new BlockPos(2, 1, 0);
        BlockPos farPos = new BlockPos(4, 1, 0);
        world.setBlockState(nearPos, Blocks.STONE.getDefaultState(), 0);
        world.setBlockState(farPos, Blocks.DIRT.getDefaultState(), 0);

        PonderScene scene = new PonderScene(world, new PonderLocalization(), "test",
            new ResourceLocation("test", "ray_trace"), Collections.emptyList(), Collections.emptyList());
        scene.begin();
        WorldSectionElement far = PonderElementFactories.get().createWorldSection(SelectionImpl.of(farPos));
        WorldSectionElement near = PonderElementFactories.get().createWorldSection(SelectionImpl.of(nearPos));
        far.setVisible(true);
        near.setVisible(true);
        scene.addElement(far);
        scene.addElement(near);

        Pair<ItemStack, BlockPos> hit = scene.rayTraceScene(
            new Vec3d(-2, 1.5, .5), new Vec3d(7, 1.5, .5));
        assertEquals(nearPos, hit.getSecond());
        assertEquals(Blocks.STONE, net.minecraft.block.Block.getBlockFromItem(hit.getFirst().getItem()));

        Pair<ItemStack, BlockPos> miss = scene.rayTraceScene(
            new Vec3d(-2, 5, .5), new Vec3d(7, 5, .5));
        assertTrue(miss.getFirst().isEmpty());
        assertNull(miss.getSecond());
    }
}
