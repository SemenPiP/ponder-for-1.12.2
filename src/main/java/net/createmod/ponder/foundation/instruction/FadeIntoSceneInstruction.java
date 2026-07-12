package net.createmod.ponder.foundation.instruction;

import net.createmod.ponder.api.element.AnimatedSceneElement;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.ElementLinkImpl;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;

public abstract class FadeIntoSceneInstruction<T extends AnimatedSceneElement> extends TickingInstruction {
    private final EnumFacing direction;
    protected final T element;
    private ElementLink<T> link;

    protected FadeIntoSceneInstruction(int ticks, EnumFacing direction, T element) {
        super(false, ticks); this.direction = direction; this.element = element;
    }
    @Override protected void firstTick(PonderScene scene) {
        scene.addElement(element); element.setVisible(true); element.forceApplyFade(totalTicks == 0 ? 1 : 0);
        element.setFadeVec(direction == null ? Vec3d.ZERO : new Vec3d(direction.getDirectionVec()).scale(.5));
        if (link != null) scene.linkElement(element, link);
    }
    @Override protected void tickRunning(PonderScene scene, int elapsed, float progress) {
        element.setFade(1 - (1 - progress) * (1 - progress));
    }
    public ElementLink<T> createLink(PonderScene scene) {
        link = new ElementLinkImpl<T>(getElementClass());
        return link;
    }
    protected abstract Class<T> getElementClass();
}
