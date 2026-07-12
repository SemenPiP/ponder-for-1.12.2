package net.createmod.ponder.foundation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.BeforeClass;
import org.junit.Test;

import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.registration.PonderLocalization;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class PonderItemEntityStabilityTest {
    private static final Vec3d LOCATION = new Vec3d(2.5, 2.2, 2.5);

    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void createItemEntityInitializesMotionInterpolationAndHoverPhase() {
        PonderLevel world = new PonderLevel(BlockPos.ORIGIN, null);
        world.backup();
        PonderScene scene = createScene(world, "item_initialization");
        PonderSceneBuilder builder = (PonderSceneBuilder) scene.builder();
        Vec3d motion = new Vec3d(.125, .25, -.375);
        builder.world().createItemEntity(LOCATION, motion, new ItemStack(Items.BOOK));
        builder.idle(1);

        scene.begin();
        scene.tick();

        EntityItem item = onlyItem(world);
        assertPositionEquals(LOCATION, item);
        assertEquals(LOCATION.x, item.prevPosX, 0);
        assertEquals(LOCATION.y, item.prevPosY, 0);
        assertEquals(LOCATION.z, item.prevPosZ, 0);
        assertEquals(LOCATION.x, item.lastTickPosX, 0);
        assertEquals(LOCATION.y, item.lastTickPosY, 0);
        assertEquals(LOCATION.z, item.lastTickPosZ, 0);
        assertEquals(motion.x, item.motionX, 0);
        assertEquals(motion.y, item.motionY, 0);
        assertEquals(motion.z, item.motionZ, 0);
        assertEquals(0, item.hoverStart, 0);
    }

    @Test
    public void zeroMotionNoGravityItemRemainsStillForOneHundredTicks() {
        PonderLevel world = new PonderLevel(BlockPos.ORIGIN, null);
        world.backup();
        PonderScene scene = createScene(world, "stationary_item");
        PonderSceneBuilder builder = (PonderSceneBuilder) scene.builder();
        ElementLink<EntityElement> itemLink = builder.world().createItemEntity(
            LOCATION, Vec3d.ZERO, new ItemStack(Items.BOOK));
        builder.world().modifyEntity(itemLink, entity -> entity.setNoGravity(true));
        builder.idle(102);

        scene.begin();
        scene.tick();
        EntityItem item = onlyItem(world);
        for (int tick = 0; tick < 100; tick++) {
            scene.tick();
            assertPositionEquals(LOCATION, item);
            assertEquals(LOCATION.x, item.prevPosX, 0);
            assertEquals(LOCATION.y, item.prevPosY, 0);
            assertEquals(LOCATION.z, item.prevPosZ, 0);
            assertEquals(LOCATION.x, item.lastTickPosX, 0);
            assertEquals(LOCATION.y, item.lastTickPosY, 0);
            assertEquals(LOCATION.z, item.lastTickPosZ, 0);
            assertEquals(0, item.motionX, 0);
            assertEquals(0, item.motionY, 0);
            assertEquals(0, item.motionZ, 0);
        }
    }

    @Test
    public void movingItemAdvancesItsInterpolationOriginEveryTick() {
        PonderLevel world = new PonderLevel(BlockPos.ORIGIN, null);
        world.backup();
        PonderScene scene = createScene(world, "moving_item_interpolation");
        PonderSceneBuilder builder = (PonderSceneBuilder) scene.builder();
        ElementLink<EntityElement> itemLink = builder.world().createItemEntity(
            LOCATION, new Vec3d(.125, .1, -.075), new ItemStack(Items.BOOK));
        builder.world().modifyEntity(itemLink, entity -> entity.setNoGravity(true));
        builder.idle(12);

        scene.begin();
        scene.tick();
        EntityItem item = onlyItem(world);
        double previousX = item.posX;
        double previousY = item.posY;
        double previousZ = item.posZ;
        for (int tick = 0; tick < 10; tick++) {
            scene.tick();
            assertEquals(previousX, item.lastTickPosX, 0);
            assertEquals(previousY, item.lastTickPosY, 0);
            assertEquals(previousZ, item.lastTickPosZ, 0);
            assertEquals(previousX, item.prevPosX, 0);
            assertEquals(previousY, item.prevPosY, 0);
            assertEquals(previousZ, item.prevPosZ, 0);
            assertTrue(item.posX != previousX || item.posY != previousY || item.posZ != previousZ);
            previousX = item.posX;
            previousY = item.posY;
            previousZ = item.posZ;
        }
    }

    @Test
    public void seekAndReplayRestoreTheSameItemPhaseAndState() {
        PonderLevel world = new PonderLevel(BlockPos.ORIGIN, null);
        world.backup();
        PonderScene scene = createScene(world, "item_replay");
        PonderSceneBuilder builder = (PonderSceneBuilder) scene.builder();
        ElementLink<EntityElement> itemLink = builder.world().createItemEntity(
            LOCATION, Vec3d.ZERO, new ItemStack(Items.BOOK));
        builder.world().modifyEntity(itemLink, entity -> entity.setNoGravity(true));
        builder.idle(5);
        builder.addKeyframe();
        builder.idle(7);

        scene.begin();
        scene.seek(10);
        EntityItem expected = onlyItem(world);
        int expectedAge = expected.getAge();
        float expectedHoverStart = expected.hoverStart;

        scene.seek(5);
        scene.seek(10);
        assertItemState(expectedAge, expectedHoverStart, onlyItem(world));

        scene.restart();
        scene.seek(10);
        assertItemState(expectedAge, expectedHoverStart, onlyItem(world));
    }

    private static PonderScene createScene(PonderLevel world, String id) {
        return new PonderScene(world, new PonderLocalization(), "test",
            new ResourceLocation("test", id), Collections.emptyList(), Collections.emptyList());
    }

    private static EntityItem onlyItem(PonderLevel world) {
        assertEquals(1, world.getEntities().size());
        assertTrue(world.getEntities().iterator().next() instanceof EntityItem);
        return (EntityItem) world.getEntities().iterator().next();
    }

    private static void assertItemState(int age, float hoverStart, EntityItem item) {
        assertEquals(age, item.getAge());
        assertEquals(hoverStart, item.hoverStart, 0);
        assertPositionEquals(LOCATION, item);
        assertEquals(0, item.motionX, 0);
        assertEquals(0, item.motionY, 0);
        assertEquals(0, item.motionZ, 0);
    }

    private static void assertPositionEquals(Vec3d expected, EntityItem item) {
        assertEquals(expected.x, item.posX, 0);
        assertEquals(expected.y, item.posY, 0);
        assertEquals(expected.z, item.posZ, 0);
    }
}
