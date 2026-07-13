package net.createmod.ponder.foundation.instruction;

import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.foundation.PonderScene;

/** One-shot deterministic visibility and removal operations for linked entities. */
public final class EntityElementInstruction extends TickingInstruction {
    private final ElementLink<EntityElement> link;
    private final Operation operation;

    private EntityElementInstruction(ElementLink<EntityElement> link, Operation operation) {
        super(false, 0);
        if (link == null || operation == null)
            throw new IllegalArgumentException("Entity link and operation are required");
        this.link = link;
        this.operation = operation;
    }

    public static EntityElementInstruction setVisible(ElementLink<EntityElement> link, boolean visible) {
        return new EntityElementInstruction(link, visible ? Operation.SHOW : Operation.HIDE);
    }

    public static EntityElementInstruction remove(ElementLink<EntityElement> link) {
        return new EntityElementInstruction(link, Operation.REMOVE);
    }

    @Override
    protected void tickRunning(PonderScene scene, int elapsed, float progress) {
        EntityElement element = scene.resolve(link);
        if (element == null)
            return;
        if (operation == Operation.SHOW) {
            element.setVisible(true);
            return;
        }
        if (operation == Operation.HIDE) {
            element.setVisible(false);
            return;
        }
        element.ifPresent(entity -> {
            if (scene.getWorld() != null)
                scene.getWorld().removeEntity(entity);
            else
                entity.setDead();
        });
        element.setVisible(false);
        scene.removeElement(element);
        scene.unlinkElement(link);
    }

    private enum Operation {
        SHOW,
        HIDE,
        REMOVE
    }
}
