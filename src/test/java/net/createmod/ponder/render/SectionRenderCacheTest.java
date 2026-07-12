package net.createmod.ponder.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import net.createmod.ponder.foundation.PonderWorld;
import net.createmod.ponder.foundation.SelectionImpl;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class SectionRenderCacheTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void selectionViewHidesOutsideNeighboursWithoutChangingSelectedState() {
        PonderWorld world = new PonderWorld(BlockPos.ORIGIN, null);
        BlockPos selected = new BlockPos(0, 1, 0);
        BlockPos outside = selected.east();
        world.setBlockState(selected, Blocks.GLASS.getDefaultState(), 0);
        world.setBlockState(outside, Blocks.STONE.getDefaultState(), 0);

        IBlockAccess view = SectionRenderCache.createSelectionView(world, SelectionImpl.of(selected));

        assertEquals(Blocks.GLASS.getDefaultState(), view.getBlockState(selected));
        assertEquals(Blocks.AIR.getDefaultState(), view.getBlockState(outside));
        assertFalse(view.isAirBlock(selected));
        assertTrue(view.isAirBlock(outside));
        assertEquals(Blocks.STONE.getDefaultState(), world.getBlockState(outside));
    }

    @Test
    public void invalidateAllMarksEveryLiveCleanCacheDirty() {
        SectionRenderCache first = new SectionRenderCache();
        SectionRenderCache second = new SectionRenderCache();
        first.markCleanForTesting();
        second.markCleanForTesting();
        assertFalse(first.isDirtyForTesting());
        assertFalse(second.isDirtyForTesting());

        SectionRenderCache.invalidateAll();

        assertTrue(first.isDirtyForTesting());
        assertTrue(second.isDirtyForTesting());
    }
}
