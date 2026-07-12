package net.createmod.ponder.foundation.element;

import java.util.Collections;

import net.createmod.ponder.api.element.MinecartElement;
import net.createmod.ponder.foundation.PonderWorld;
import net.createmod.ponder.render.PonderWorldRenderer;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.util.math.Vec3d;

public class MinecartElementImpl extends AnimatedSceneElementBase implements MinecartElement {
    private static final PonderWorldRenderer RENDERER=new PonderWorldRenderer();
    private Vec3d previousPosition;
    private Vec3d position;
    private float previousYaw;
    private float yaw;
    private final MinecartConstructor constructor;
    private EntityMinecart minecart;
    public MinecartElementImpl(Vec3d location,float angle,MinecartConstructor constructor){previousPosition=position=location;previousYaw=yaw=angle;this.constructor=constructor;}
    @Override public void setPositionOffset(Vec3d value,boolean immediate){Vec3d next=value==null?Vec3d.ZERO:value;previousPosition=immediate?next:position;position=next;}
    @Override public void setRotation(float value,boolean immediate){previousYaw=immediate?value:yaw;yaw=value;}
    @Override public Vec3d getPositionOffset(){return position;}
    @Override public Vec3d getRotation(){return new Vec3d(0,yaw,0);}
    @Override public void renderLast(PonderWorld world,float partialTicks){
        if(minecart==null){minecart=constructor.create(world,position.x,position.y,position.z);minecart.setWorld(world);}
        Vec3d at=lerp(previousPosition,position,partialTicks);
        float atYaw=previousYaw+(yaw-previousYaw)*partialTicks;
        minecart.setPosition(at.x,at.y,at.z);
        minecart.prevPosX=minecart.lastTickPosX=minecart.posX=at.x;
        minecart.prevPosY=minecart.lastTickPosY=minecart.posY=at.y;
        minecart.prevPosZ=minecart.lastTickPosZ=minecart.posZ=at.z;
        minecart.prevRotationYaw=minecart.rotationYaw=atYaw;
        RENDERER.renderEntities(Collections.<net.minecraft.entity.Entity>singleton(minecart),Vec3d.ZERO,partialTicks);
    }
    private static Vec3d lerp(Vec3d a,Vec3d b,float t){return new Vec3d(a.x+(b.x-a.x)*t,a.y+(b.y-a.y)*t,a.z+(b.z-a.z)*t);}
    @Override public Object captureState(){return new State(super.captureState(),previousPosition,position,previousYaw,yaw);}
    @Override public void restoreState(Object value){if(!(value instanceof State)){super.restoreState(value);return;}State s=(State)value;
        super.restoreState(s.animation);previousPosition=s.previousPosition;position=s.position;previousYaw=s.previousYaw;yaw=s.yaw;minecart=null;}
    private static final class State{final Object animation;final Vec3d previousPosition,position;final float previousYaw,yaw;
        State(Object a,Vec3d pp,Vec3d p,float py,float y){animation=a;previousPosition=pp;position=p;previousYaw=py;yaw=y;}}
}
