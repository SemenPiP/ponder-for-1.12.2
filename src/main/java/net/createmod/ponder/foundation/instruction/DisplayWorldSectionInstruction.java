package net.createmod.ponder.foundation.instruction;

import java.util.function.Supplier;

import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderElementFactories;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.ElementLinkImpl;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class DisplayWorldSectionInstruction extends TickingInstruction {
    private final EnumFacing direction;
    private final Selection selection;
    private final Supplier<WorldSectionElement> target;
    private final BlockPos gluedPosition;
    private final ElementLinkImpl<WorldSectionElement> link = new ElementLinkImpl<WorldSectionElement>(WorldSectionElement.class);
    private WorldSectionElement element;

    public DisplayWorldSectionInstruction(int ticks, EnumFacing direction, Selection selection,
                                          Supplier<WorldSectionElement> target) {
        this(ticks, direction, selection, target, null);
    }

    public DisplayWorldSectionInstruction(int ticks, EnumFacing direction, Selection selection,
                                          Supplier<WorldSectionElement> target, BlockPos gluedPosition) {
        super(false, ticks);
        this.direction = direction == null ? EnumFacing.UP : direction;
        this.selection = selection.copy();
        this.target = target;
        this.gluedPosition = gluedPosition;
    }

    public ElementLink<WorldSectionElement> createLink(PonderScene scene) {
        return link;
    }

    @Override protected void firstTick(PonderScene scene) {
        element = target == null ? null : target.get();
        if (element == null) {
            element = PonderElementFactories.get().createWorldSection(selection);
            scene.addElement(element);
            scene.linkElement(element, link);
        } else {
            element.add(selection);
        }
        if (gluedPosition != null)
            element.setCenterOfRotation(new Vec3d(gluedPosition).add(.5, .5, .5));
        element.setFadeVec(new Vec3d(direction.getDirectionVec()));
        element.forceApplyFade(totalTicks == 0 ? 1 : 0);
        element.setVisible(true);
        element.queueRedraw();
    }

    @Override protected void tickRunning(PonderScene scene, int elapsed, float progress) {
        if (element != null) element.setFade(smooth(progress));
    }

    private static float smooth(float value) { return value * value * (3 - 2 * value); }
}
