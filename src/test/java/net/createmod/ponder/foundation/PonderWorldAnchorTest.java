package net.createmod.ponder.foundation;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.BeforeClass;
import org.junit.Test;

import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.registration.PonderLocalization;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class PonderWorldAnchorTest {
    @BeforeClass public static void bootstrapMinecraft() { Bootstrap.register(); }

    @Test
    public void nonZeroAnchorConvertsExactlyOnceAndSceneTransformIsInvertible() {
        BlockPos anchor = new BlockPos(96, 64, -32);
        BlockPos local = new BlockPos(2, 3, 4);
        PonderLevel world = new PonderLevel(anchor, null);

        assertEquals(anchor.add(local), world.localToWorld(local));
        assertEquals(local, world.worldToLocal(anchor.add(local)));
        world.setBlockState(local, Blocks.STONE.getDefaultState(), 0);
        assertEquals(Blocks.STONE, world.getBlockState(local).getBlock());
        assertEquals(Blocks.AIR, world.getBlockState(anchor.add(local)).getBlock());

        PonderScene scene = new PonderScene(world, new PonderLocalization(), "test",
            new ResourceLocation("test", "anchor"), Collections.emptyList(), Collections.emptyList());
        Vec3d localPoint = new Vec3d(2.25, 3.5, 4.75);
        Vec3d worldPoint = scene.getTransform().toWorld(localPoint);
        assertVector(new Vec3d(98.25, 67.5, -27.25), worldPoint);
        assertVector(localPoint, scene.getTransform().toLocal(worldPoint));
    }

    private static void assertVector(Vec3d expected, Vec3d actual) {
        assertEquals(expected.x, actual.x, 1e-6);
        assertEquals(expected.y, actual.y, 1e-6);
        assertEquals(expected.z, actual.z, 1e-6);
    }
}
