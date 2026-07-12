package net.createmod.ponder.render;

import net.createmod.catnip.outliner.AABBOutline;
import net.createmod.catnip.outliner.BlockClusterOutline;
import net.createmod.catnip.outliner.LineOutline;
import net.createmod.catnip.outliner.Outline;
import net.createmod.ponder.foundation.element.OverlayDataElement;
import net.minecraft.util.math.Vec3d;

/** Converts render-neutral overlay instructions into fixed-pipeline outlines. */
public final class PonderOverlayRenderer {
    private PonderOverlayRenderer() {}

    public static void render(OverlayDataElement data, Vec3d camera, float partialTicks) {
        if (data == null || !data.isVisible()) return;
        Outline outline;
        switch (data.getKind()) {
            case LINE:
            case BIG_LINE:
                if (data.getStart() == null || data.getEnd() == null) return;
                outline = new LineOutline(data.getStart(), data.getEnd());
                outline.getParams().lineWidth(data.getKind() == OverlayDataElement.Kind.BIG_LINE ? .125f : .03125f);
                break;
            case SELECTION_OUTLINE:
                if (data.getSelection() == null) return;
                outline = new BlockClusterOutline(data.getSelection());
                outline.getParams().lineWidth(.04f);
                break;
            case BOUNDING_BOX:
                if (data.getBounds() == null) return;
                outline = new AABBOutline(data.getBounds());
                outline.getParams().lineWidth(.04f);
                break;
            default:
                return;
        }
        outline.getParams().colored(data.getColor().getColor());
        outline.render(camera, partialTicks);
    }
}
