package net.createmod.ponder.foundation.element;

import java.util.Collections;

import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.foundation.PonderWorld;
import net.createmod.ponder.render.PonderWorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.createmod.ponder.foundation.PonderScene;
import java.util.UUID;

public class EntityElementImpl extends TrackedElementBase<Entity> implements EntityElement {
    private static final PonderWorldRenderer RENDERER=new PonderWorldRenderer();
    private final UUID entityId;
    public EntityElementImpl(Entity entity){super(entity);entityId=entity.getUniqueID();}
    @Override public void tick(PonderScene scene){
        if(tracked!=null&&!tracked.isDead)return;
        if(scene.getWorld()!=null)for(Entity entity:scene.getWorld().getEntities())if(entityId.equals(entity.getUniqueID())){tracked=entity;break;}
    }
    @Override public boolean isStillValid(Entity entity){return !entity.isDead;}
    @Override public void renderLast(PonderWorld world,float partialTicks){
        if(tracked==null||tracked.isDead)return;
        if(tracked.world!=world)tracked.setWorld(world);
        RENDERER.renderEntities(Collections.singleton(tracked),Vec3d.ZERO,partialTicks);
    }
}
