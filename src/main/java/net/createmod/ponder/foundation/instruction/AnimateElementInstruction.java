package net.createmod.ponder.foundation.instruction;

import java.util.function.BiConsumer;
import java.util.function.Function;

import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.PonderSceneElement;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.util.math.Vec3d;

public class AnimateElementInstruction<T extends PonderSceneElement> extends TickingInstruction {
    private final ElementLink<T> link;
    private final Vec3d totalDelta;
    private final BiConsumer<T, Vec3d> setter;
    private final Function<T, Vec3d> getter;
    private T element;
    private Vec3d start = Vec3d.ZERO;

    public AnimateElementInstruction(ElementLink<T> link, Vec3d totalDelta, int ticks,
                                     BiConsumer<T, Vec3d> setter, Function<T, Vec3d> getter) {
        super(false, ticks);
        this.link = link; this.totalDelta = totalDelta; this.setter = setter; this.getter = getter;
    }

    @Override protected void firstTick(PonderScene scene) {
        element = scene.resolve(link);
        if (element != null) start = getter.apply(element);
    }

    @Override protected void tickRunning(PonderScene scene, int elapsed, float progress) {
        if (element != null) setter.accept(element, start.add(totalDelta.scale(progress * progress * (3 - 2 * progress))));
    }
}
