package net.createmod.ponder.foundation.element;

import java.util.Collections;
import java.util.function.Supplier;

import net.createmod.ponder.api.element.ParrotElement;
import net.createmod.ponder.api.element.ParrotPose;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.PonderWorld;
import net.createmod.ponder.render.PonderWorldRenderer;
import net.minecraft.entity.passive.EntityParrot;
import net.minecraft.util.math.Vec3d;

public class ParrotElementImpl extends AnimatedSceneElementBase implements ParrotElement {
    private static final PonderWorldRenderer RENDERER=new PonderWorldRenderer();
    private Vec3d previousPosition;
    private Vec3d position;
    private Vec3d previousRotation=Vec3d.ZERO;
    private Vec3d rotation=Vec3d.ZERO;
    private ParrotPose pose;
    private EntityParrot parrot;
    public ParrotElementImpl(Vec3d location,Supplier<? extends ParrotPose> supplier){previousPosition=position=location;pose=supplier.get();}
    @Override public void setPositionOffset(Vec3d value,boolean immediate){Vec3d next=value==null?Vec3d.ZERO:value;previousPosition=immediate?next:position;position=next;}
    @Override public void setRotation(Vec3d value,boolean immediate){Vec3d next=value==null?Vec3d.ZERO:value;previousRotation=immediate?next:rotation;rotation=next;}
    @Override public Vec3d getPositionOffset(){return position;}
    @Override public Vec3d getRotation(){return rotation;}
    @Override public void setPose(ParrotPose value){if(value!=null)pose=value;}
    @Override public void tick(PonderScene scene){if(parrot!=null&&pose!=null)pose.tick(scene,parrot,position);}
    @Override public void renderLast(PonderWorld world,float partialTicks){
        if(parrot==null){parrot=new EntityParrot(world);parrot.setVariant(Math.abs(world.getAnchor().hashCode())%5);}
        Vec3d at=lerp(previousPosition,position,partialTicks);Vec3d angle=lerp(previousRotation,rotation,partialTicks);
        parrot.setPosition(at.x,at.y,at.z);parrot.prevRotationYaw=(float)previousRotation.y;parrot.rotationYaw=(float)angle.y;parrot.rotationPitch=(float)angle.x;
        RENDERER.renderEntities(Collections.<net.minecraft.entity.Entity>singleton(parrot),Vec3d.ZERO,partialTicks);
    }
    private static Vec3d lerp(Vec3d a,Vec3d b,float t){return new Vec3d(a.x+(b.x-a.x)*t,a.y+(b.y-a.y)*t,a.z+(b.z-a.z)*t);}
    @Override public Object captureState(){return new State(super.captureState(),previousPosition,position,previousRotation,rotation,pose);}
    @Override public void restoreState(Object value){if(!(value instanceof State)){super.restoreState(value);return;}State s=(State)value;
        super.restoreState(s.animation);previousPosition=s.previousPosition;position=s.position;previousRotation=s.previousRotation;
        rotation=s.rotation;pose=s.pose;parrot=null;}
    private static final class State{final Object animation;final Vec3d previousPosition,position,previousRotation,rotation;final ParrotPose pose;
        State(Object a,Vec3d pp,Vec3d p,Vec3d pr,Vec3d r,ParrotPose pose){animation=a;previousPosition=pp;position=p;previousRotation=pr;rotation=r;this.pose=pose;}}
}
