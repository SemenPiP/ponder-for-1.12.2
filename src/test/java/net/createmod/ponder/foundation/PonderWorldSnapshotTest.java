package net.createmod.ponder.foundation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;

public class PonderWorldSnapshotTest {
    @BeforeClass public static void bootstrapMinecraft() { Bootstrap.register(); }

    @Test
    public void assignsUniqueRuntimeEntityIds() {
        PonderLevel world = new PonderLevel(BlockPos.ORIGIN, null);
        EntityItem first = new EntityItem(world, 0, 1, 0, new ItemStack(Items.STICK));
        EntityItem second = new EntityItem(world, 1, 1, 0, new ItemStack(Items.STICK));
        first.setEntityId(0); second.setEntityId(0);
        assertTrue(world.spawnEntity(first)); assertTrue(world.spawnEntity(second));
        assertTrue(first.getEntityId() > 0); assertTrue(second.getEntityId() > 0);
        assertNotEquals(first.getEntityId(), second.getEntityId());
        assertSame(first, world.getEntityByID(first.getEntityId()));
        assertSame(second, world.getEntityByID(second.getEntityId()));
    }

    @Test
    public void snapshotRestoresPendingParticles() {
        PonderLevel world = new PonderLevel(BlockPos.ORIGIN, null);
        world.spawnParticle(EnumParticleTypes.FLAME, 1, 2, 3, 0, .1, 0);
        PonderWorld.Snapshot snapshot = world.createSnapshot();
        assertEquals(1, world.drainParticles().size());
        world.restoreSnapshot(snapshot);
        assertEquals(1, world.drainParticles().size());
    }

    @Test
    public void snapshotRestoresItemHoverStart() {
        PonderLevel world = new PonderLevel(BlockPos.ORIGIN, null);
        EntityItem item = new EntityItem(world, .5, 2, .5, new ItemStack(Items.BOOK));
        item.hoverStart = 1.2345F;
        assertTrue(world.spawnEntity(item));

        PonderWorld.Snapshot snapshot = world.createSnapshot();
        item.hoverStart = 5.6789F;
        world.restoreSnapshot(snapshot);

        EntityItem restored = (EntityItem) world.getEntities().iterator().next();
        assertEquals(1.2345F, restored.hoverStart, 0);
    }
}
