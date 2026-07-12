package net.createmod.ponder.api.element;

import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public interface MinecartElement extends AnimatedSceneElement {
    void setPositionOffset(Vec3d position, boolean immediate);
    void setRotation(float angle, boolean immediate);
    Vec3d getPositionOffset();
    Vec3d getRotation();

    interface MinecartConstructor {
        EntityMinecart create(World world, double x, double y, double z);
    }
}
