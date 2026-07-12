package net.createmod.ponder.foundation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;

import org.junit.BeforeClass;
import org.junit.Test;

import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.registration.PonderLocalization;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class PonderSceneKeyframeStateTest {
    private static final BlockPos BLOCK = new BlockPos(1, 1, 1);
    private static final BlockPos CHEST = new BlockPos(2, 1, 1);

    @BeforeClass public static void bootstrapMinecraft() { Bootstrap.register(); }

    @Test
    public void keyframeRestoresSceneWorldSectionEntityTileAndParticleState() {
        PonderLevel world = createWorld();
        PonderScene scene = new PonderScene(world, new PonderLocalization(), "test",
            new ResourceLocation("test", "complete_keyframe"), Collections.emptyList(), Collections.emptyList());
        PonderSceneBuilder builder = (PonderSceneBuilder) scene.builder();
        builder.title("complete_keyframe", "Complete Keyframe");

        ElementLink<WorldSectionElement> section = builder.world().showIndependentSectionImmediately(
            SelectionImpl.of(BLOCK));
        builder.world().moveSection(section, new Vec3d(2, 0, 0), 4);
        builder.world().incrementBlockBreakingProgress(BLOCK);
        builder.special().movePointOfInterest(new Vec3d(3, 4, 5));
        builder.addInstruction(value -> value.setCursorPosition(new Vec3d(6, 7, 8)));
        builder.rotateCameraY(40);
        builder.effects().emitParticles(new Vec3d(1.5, 2, 1.5),
            builder.effects().simpleParticleEmitter(EnumParticleTypes.FLAME, Vec3d.ZERO), 1, 4);
        builder.idle(4);
        builder.addKeyframe();

        builder.world().setBlock(BLOCK, Blocks.GOLD_BLOCK.getDefaultState(), false);
        builder.world().modifyBlockEntity(CHEST, TileEntityChest.class,
            chest -> chest.setInventorySlotContents(0, new ItemStack(Items.DIAMOND)));
        builder.world().createItemEntity(new Vec3d(3, 2, 1), Vec3d.ZERO, new ItemStack(Items.APPLE));
        builder.world().moveSection(section, new Vec3d(0, 2, 0), 3);
        builder.world().incrementBlockBreakingProgress(BLOCK);
        builder.special().movePointOfInterest(new Vec3d(9, 9, 9));
        builder.addInstruction(value -> value.setCursorPosition(new Vec3d(10, 10, 10)));
        builder.rotateCameraY(20);
        builder.idle(3);
        builder.markAsFinished();

        scene.begin();
        scene.seek(4);
        WorldSectionElement atKeyframe = scene.resolve(section);
        assertNotNull(atKeyframe);
        Vec3d keyframeOffset = atKeyframe.getAnimatedOffset();
        float keyframeYaw = scene.getCameraYaw();
        long keyframeWorldTime = world.getWorldTime();
        assertEquals(new Vec3d(2, 0, 0), keyframeOffset);
        assertEquals(Blocks.STONE, world.getBlockState(BLOCK).getBlock());
        assertEquals(Items.STICK, chest(world).getStackInSlot(0).getItem());
        assertEquals(1, world.getEntities().size());
        assertEquals(0, breakProgress(world));

        scene.seek(7);
        assertEquals(Blocks.GOLD_BLOCK, world.getBlockState(BLOCK).getBlock());
        assertEquals(Items.DIAMOND, chest(world).getStackInSlot(0).getItem());
        assertEquals(2, world.getEntities().size());
        assertEquals(new Vec3d(2, 2, 0), scene.resolve(section).getAnimatedOffset());
        assertEquals(1, breakProgress(world));
        assertNotEquals(keyframeYaw, scene.getCameraYaw(), 1e-6f);
        assertEquals(4, world.drainParticles().size());

        scene.seek(4);
        assertEquals(Blocks.STONE, world.getBlockState(BLOCK).getBlock());
        assertEquals(Items.STICK, chest(world).getStackInSlot(0).getItem());
        assertEquals(1, world.getEntities().size());
        assertEquals(new Vec3d(2, 0, 0), scene.resolve(section).getAnimatedOffset());
        assertEquals(new Vec3d(3, 4, 5), scene.getPointOfInterest());
        assertEquals(new Vec3d(6, 7, 8), scene.getCursorPosition());
        assertEquals(keyframeYaw, scene.getCameraYaw(), 1e-6f);
        assertEquals(keyframeWorldTime, world.getWorldTime());
        assertEquals(0, breakProgress(world));
        assertEquals(4, world.drainParticles().size());
    }

    private static PonderLevel createWorld() {
        PonderLevel world = new PonderLevel(BlockPos.ORIGIN, null);
        world.setBlockState(BLOCK, Blocks.STONE.getDefaultState(), 0);
        world.setBlockState(CHEST, Blocks.CHEST.getDefaultState(), 0);
        TileEntityChest chest = new TileEntityChest();
        chest.setInventorySlotContents(0, new ItemStack(Items.STICK));
        world.setTileEntity(CHEST, chest);
        world.spawnEntity(new EntityItem(world, .5, 2, .5, new ItemStack(Items.BOOK)));
        world.backup();
        return world;
    }

    private static TileEntityChest chest(PonderLevel world) {
        return (TileEntityChest) world.getTileEntity(CHEST);
    }

    private static int breakProgress(PonderLevel world) {
        return world.getBlockBreakingProgress().values().iterator().next().progress;
    }
}
