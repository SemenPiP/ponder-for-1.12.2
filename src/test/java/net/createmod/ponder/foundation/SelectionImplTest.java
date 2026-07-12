package net.createmod.ponder.foundation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import net.createmod.ponder.api.scene.Selection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class SelectionImplTest {
    @Test
    public void compoundOperationsInvalidateCenterAndRemainDeterministic() {
        Selection selection = SelectionImpl.of(new BlockPos(0, 0, 0), new BlockPos(1, 0, 0));
        assertEquals(new Vec3d(1, .5, .5), selection.getCenter());
        selection.add(SelectionImpl.of(new BlockPos(2, 0, 0)));
        assertEquals(new Vec3d(1.5, .5, .5), selection.getCenter());
        selection.substract(SelectionImpl.of(new BlockPos(1, 0, 0)));
        assertEquals(2, selection.size());
        assertTrue(selection.test(new BlockPos(0, 0, 0)));
        assertFalse(selection.test(new BlockPos(1, 0, 0)));
        assertEquals(new BlockPos(0, 0, 0), selection.iterator().next());
    }

    @Test
    public void copyDoesNotShareMutableMembership() {
        Selection original = SelectionImpl.of(new BlockPos(0, 0, 0));
        Selection copy = original.copy().add(SelectionImpl.of(new BlockPos(1, 0, 0)));
        assertEquals(1, original.size());
        assertEquals(2, copy.size());
    }
}
