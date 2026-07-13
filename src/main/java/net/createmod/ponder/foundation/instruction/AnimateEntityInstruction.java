package net.createmod.ponder.foundation.instruction;

import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

/** Deterministic entity movement used by the script IR without exposing callbacks. */
public final class AnimateEntityInstruction extends TickingInstruction {
    private final ElementLink<EntityElement> link;
    private final Vec3d delta;
    private EntityElement element;
    private Vec3d start;

    public AnimateEntityInstruction(ElementLink<EntityElement> link, Vec3d delta, int duration) {
        super(false, duration);
        if (link == null || delta == null)
            throw new IllegalArgumentException("Entity link and movement offset are required");
        this.link = link;
        this.delta = delta;
    }

    @Override
    protected void firstTick(PonderScene scene) {
        element = scene.resolve(link);
        start = Vec3d.ZERO;
        if (element != null)
            element.ifPresent(entity -> start = entity.getPositionVector());
    }

    @Override
    protected void tickRunning(PonderScene scene, int elapsed, float progress) {
        if (element == null)
            return;
        double eased = progress * progress * (3 - 2 * progress);
        Vec3d target = start.add(delta.scale(eased));
        element.ifPresent(entity -> setPosition(entity, target, progress >= 1));
    }

    private static void setPosition(Entity entity, Vec3d position, boolean immediate) {
        if (!immediate) {
            entity.prevPosX = entity.lastTickPosX = entity.posX;
            entity.prevPosY = entity.lastTickPosY = entity.posY;
            entity.prevPosZ = entity.lastTickPosZ = entity.posZ;
        }
        entity.setPosition(position.x, position.y, position.z);
        if (immediate) {
            entity.prevPosX = entity.lastTickPosX = position.x;
            entity.prevPosY = entity.lastTickPosY = position.y;
            entity.prevPosZ = entity.lastTickPosZ = position.z;
        }
    }
}
