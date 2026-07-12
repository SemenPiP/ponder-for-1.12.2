package net.createmod.ponder.foundation.element;

import javax.annotation.Nullable;

import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.PonderOverlayElement;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

/** Render-neutral description of line and outline overlays consumed by the 1.12 renderer. */
public final class OverlayDataElement implements PonderOverlayElement {
    public enum Kind { LINE, BIG_LINE, SELECTION_OUTLINE, BOUNDING_BOX }

    private final Kind kind;
    private final PonderPalette color;
    @Nullable private final Object slot;
    @Nullable private final Vec3d start;
    @Nullable private final Vec3d end;
    @Nullable private final Selection selection;
    @Nullable private final AxisAlignedBB bounds;
    private boolean visible;

    private OverlayDataElement(Kind kind, PonderPalette color, @Nullable Object slot,
                               @Nullable Vec3d start, @Nullable Vec3d end,
                               @Nullable Selection selection, @Nullable AxisAlignedBB bounds) {
        this.kind = kind;
        this.color = color;
        this.slot = slot;
        this.start = start;
        this.end = end;
        this.selection = selection == null ? null : selection.copy();
        this.bounds = bounds;
    }

    public static OverlayDataElement line(PonderPalette color, Vec3d start, Vec3d end, boolean large) {
        return new OverlayDataElement(large ? Kind.BIG_LINE : Kind.LINE, color, null, start, end, null, null);
    }

    public static OverlayDataElement outline(PonderPalette color, Object slot, Selection selection) {
        return new OverlayDataElement(Kind.SELECTION_OUTLINE, color, slot, null, null, selection, null);
    }

    public static OverlayDataElement bounds(PonderPalette color, Object slot, AxisAlignedBB bounds) {
        return new OverlayDataElement(Kind.BOUNDING_BOX, color, slot, null, null, null, bounds);
    }

    public Kind getKind() { return kind; }
    public PonderPalette getColor() { return color; }
    @Nullable public Object getSlot() { return slot; }
    @Nullable public Vec3d getStart() { return start; }
    @Nullable public Vec3d getEnd() { return end; }
    @Nullable public Selection getSelection() { return selection == null ? null : selection.copy(); }
    @Nullable public AxisAlignedBB getBounds() { return bounds; }
    @Override public boolean isVisible() { return visible; }
    @Override public void setVisible(boolean visible) { this.visible = visible; }
    @Override public void render(PonderScene scene, int mouseX, int mouseY, float partialTicks) { }
}
