package net.createmod.ponder.api.scene;

import java.util.function.Supplier;

import net.createmod.ponder.api.element.AnimatedSceneElement;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.MinecartElement;
import net.createmod.ponder.api.element.ParrotElement;
import net.createmod.ponder.api.element.ParrotPose;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public interface SpecialInstructions {
    ElementLink<ParrotElement> createBirb(Vec3d location, Supplier<? extends ParrotPose> pose);
    void changeBirbPose(ElementLink<ParrotElement> birb, Supplier<? extends ParrotPose> pose);
    void movePointOfInterest(Vec3d location);
    void movePointOfInterest(BlockPos location);
    void rotateParrot(ElementLink<ParrotElement> link, double xRotation, double yRotation,
                      double zRotation, int duration);
    void moveParrot(ElementLink<ParrotElement> link, Vec3d offset, int duration);
    ElementLink<MinecartElement> createCart(Vec3d location, float angle,
                                            MinecartElement.MinecartConstructor type);
    void rotateCart(ElementLink<MinecartElement> link, float yRotation, int duration);
    void moveCart(ElementLink<MinecartElement> link, Vec3d offset, int duration);
    <T extends AnimatedSceneElement> void hideElement(ElementLink<T> link, EnumFacing direction);
}
