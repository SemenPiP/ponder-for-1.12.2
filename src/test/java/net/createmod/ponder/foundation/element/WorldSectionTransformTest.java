package net.createmod.ponder.foundation.element;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Method;

import org.junit.BeforeClass;
import org.junit.Test;

import net.createmod.catnip.data.Pair;
import net.createmod.ponder.foundation.PonderWorld;
import net.createmod.ponder.foundation.SelectionImpl;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

public class WorldSectionTransformTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void stableAnchorCompensatesRotationWithoutCancellingTranslation() {
        PonderWorld world = new PonderWorld(BlockPos.ORIGIN, null);
        BlockPos pos = new BlockPos(1, 1, 0);
        world.setBlockState(pos, Blocks.STONE.getDefaultState(), 0);
        WorldSectionElementImpl section = new WorldSectionElementImpl(SelectionImpl.of(pos));
        section.setCenterOfRotation(Vec3d.ZERO);
        section.stabilizeRotation(new Vec3d(1, 0, 0));
        section.setAnimatedRotation(new Vec3d(0, 90, 0), true);
        section.setAnimatedOffset(new Vec3d(2, 0, 0), true);

        Pair<Vec3d, RayTraceResult> hit = section.rayTrace(world,
            new Vec3d(3.5, 1.5, -3), new Vec3d(3.5, 1.5, 2), 1);
        assertNotNull(hit);
        assertEquals(pos, hit.getSecond().getBlockPos());
        assertEquals(EnumFacing.NORTH, hit.getSecond().sideHit);
        assertEquals(3.5, hit.getFirst().x, 1e-6);
        assertEquals(-1, hit.getFirst().z, 1e-6);
    }

    @Test
    public void renderTransformAndInverseRoundTripWithInterpolatedFade() throws Exception {
        WorldSectionElementImpl section = new WorldSectionElementImpl(
            SelectionImpl.of(new BlockPos(2, 1, -1)));
        section.setCenterOfRotation(Vec3d.ZERO);
        section.setAnimatedRotation(Vec3d.ZERO, true);
        section.setAnimatedRotation(new Vec3d(0, 90, 0), false);
        section.setAnimatedOffset(Vec3d.ZERO, true);
        section.setAnimatedOffset(new Vec3d(2, 0, 0), false);
        section.setFadeVec(new Vec3d(0, 2, 0));
        section.forceApplyFade(1);
        section.setFade(0);

        Method getTransform = WorldSectionElementImpl.class.getDeclaredMethod("getTransform", float.class);
        getTransform.setAccessible(true);
        Object transform = getTransform.invoke(section, .5f);
        Method forward = transform.getClass().getDeclaredMethod("transform", Vec3d.class);
        Method inverse = transform.getClass().getDeclaredMethod("inverse", Vec3d.class);
        forward.setAccessible(true);
        inverse.setAccessible(true);
        Vec3d source = new Vec3d(1, 0, 0);
        Vec3d transformed = (Vec3d) forward.invoke(transform, source);
        double rootHalf = Math.sqrt(.5);
        assertEquals(1 + rootHalf, transformed.x, 1e-6);
        assertEquals(1, transformed.y, 1e-6);
        assertEquals(-rootHalf, transformed.z, 1e-6);
        Vec3d restored = (Vec3d) inverse.invoke(transform, transformed);
        assertEquals(source.x, restored.x, 1e-6);
        assertEquals(source.y, restored.y, 1e-6);
        assertEquals(source.z, restored.z, 1e-6);
    }

    @Test
    public void productionRayTraceUsesTheBlocksSelectedShapeInsteadOfAFullCube() {
        PonderWorld world = new PonderWorld(BlockPos.ORIGIN, null);
        BlockPos pos = new BlockPos(0, 1, 0);
        world.setBlockState(pos, Blocks.TORCH.getDefaultState(), 0);
        WorldSectionElementImpl section = new WorldSectionElementImpl(SelectionImpl.of(pos));

        assertNull(section.rayTrace(world, new Vec3d(.95, 1.5, -1),
            new Vec3d(.95, 1.5, 2), 1));
        assertNotNull(section.rayTrace(world, new Vec3d(.5, 1.5, -1),
            new Vec3d(.5, 1.5, 2), 1));
    }
}
