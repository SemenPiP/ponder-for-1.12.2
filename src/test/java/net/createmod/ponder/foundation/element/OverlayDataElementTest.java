package net.createmod.ponder.foundation.element;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import org.junit.Test;

import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.SelectionImpl;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class OverlayDataElementTest {
    @Test
    public void lineFactoriesPreserveKindAndEndpoints() {
        Vec3d start = new Vec3d(1, 2, 3);
        Vec3d end = new Vec3d(4, 5, 6);
        OverlayDataElement line = OverlayDataElement.line(PonderPalette.BLUE, start, end, false);
        OverlayDataElement big = OverlayDataElement.line(PonderPalette.RED, start, end, true);
        assertEquals(OverlayDataElement.Kind.LINE, line.getKind());
        assertEquals(OverlayDataElement.Kind.BIG_LINE, big.getKind());
        assertEquals(start, line.getStart());
        assertEquals(end, line.getEnd());
    }

    @Test
    public void outlineOwnsDefensiveSelectionCopies() {
        Selection source = SelectionImpl.of(new BlockPos(0, 0, 0));
        OverlayDataElement outline = OverlayDataElement.outline(PonderPalette.GREEN, "slot", source);
        source.add(SelectionImpl.of(new BlockPos(1, 0, 0)));
        assertEquals(1, outline.getSelection().size());
        Selection returned = outline.getSelection();
        returned.add(SelectionImpl.of(new BlockPos(2, 0, 0)));
        assertEquals(1, outline.getSelection().size());
        assertNotSame(returned, outline.getSelection());
    }

    @Test
    public void boundsFactoryRetainsExactBox() {
        AxisAlignedBB bounds = new AxisAlignedBB(-1, 2, 3, 4, 5, 6);
        OverlayDataElement overlay = OverlayDataElement.bounds(PonderPalette.WHITE, "box", bounds);
        assertEquals(OverlayDataElement.Kind.BOUNDING_BOX, overlay.getKind());
        assertEquals(bounds, overlay.getBounds());
        assertEquals("box", overlay.getSlot());
    }
}
