package net.createmod.ponder.client;

import java.util.function.Supplier;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.element.InputElementBuilder;
import net.createmod.ponder.api.element.MinecartElement;
import net.createmod.ponder.api.element.ParrotElement;
import net.createmod.ponder.api.element.ParrotPose;
import net.createmod.ponder.api.element.TextElementBuilder;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderElementFactory;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.EntityElementImpl;
import net.createmod.ponder.foundation.element.InputWindowElement;
import net.createmod.ponder.foundation.element.MinecartElementImpl;
import net.createmod.ponder.foundation.element.ParrotElementImpl;
import net.createmod.ponder.foundation.element.TextWindowElement;
import net.createmod.ponder.foundation.element.WorldSectionElementImpl;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public final class ClientPonderElementFactory implements PonderElementFactory {
    @Override public WorldSectionElement createWorldSection(Selection selection){return new WorldSectionElementImpl(selection);}
    @Override public EntityElement createEntity(Entity entity){return new EntityElementImpl(entity);}
    @Override public TextElementBuilder createText(PonderScene scene,int duration){return new TextWindowElement(scene,duration);}
    @Override public InputElementBuilder createInput(PonderScene scene,Vec3d location,Pointing direction,int duration){return new InputWindowElement(scene,location,direction,duration);}
    @Override public ParrotElement createParrot(Vec3d location,Supplier<? extends ParrotPose> pose){return new ParrotElementImpl(location,pose);}
    @Override public MinecartElement createMinecart(Vec3d location,float angle,MinecartElement.MinecartConstructor constructor){return new MinecartElementImpl(location,angle,constructor);}
}
