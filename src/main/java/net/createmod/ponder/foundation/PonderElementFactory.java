package net.createmod.ponder.foundation;

import java.util.function.Supplier;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.element.InputElementBuilder;
import net.createmod.ponder.api.element.MinecartElement;
import net.createmod.ponder.api.element.ParrotElement;
import net.createmod.ponder.api.element.ParrotPose;
import net.createmod.ponder.api.element.PonderElement;
import net.createmod.ponder.api.element.TextElementBuilder;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

/** Client code installs a rendering implementation; the default is deterministic and headless. */
public interface PonderElementFactory {
    WorldSectionElement createWorldSection(Selection selection);
    EntityElement createEntity(Entity entity);
    TextElementBuilder createText(PonderScene scene, int duration);
    InputElementBuilder createInput(PonderScene scene, Vec3d location, Pointing direction, int duration);
    ParrotElement createParrot(Vec3d location, Supplier<? extends ParrotPose> pose);
    MinecartElement createMinecart(Vec3d location, float angle, MinecartElement.MinecartConstructor constructor);

    default PonderElement asElement(Object builder) {
        if (!(builder instanceof PonderElement))
            throw new IllegalStateException("Ponder element factory returned a builder which is not a PonderElement");
        return (PonderElement) builder;
    }
}
