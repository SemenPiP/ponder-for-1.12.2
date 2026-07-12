package net.createmod.ponder.api.element;

import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.entity.passive.EntityParrot;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public abstract class ParrotPose {
    public abstract void tick(PonderScene scene, EntityParrot entity, Vec3d location);

    public EntityParrot create(PonderLevel world) {
        EntityParrot parrot = new EntityParrot(world);
        parrot.setVariant(Math.abs((int) (world.getTotalWorldTime() + locationHash(world))) % 5);
        return parrot;
    }

    private static int locationHash(PonderLevel world) {
        return world.getAnchor().hashCode();
    }

    public static class DancePose extends ParrotPose {
        @Override public void tick(PonderScene scene, EntityParrot entity, Vec3d location) {
            entity.prevRotationYaw = entity.rotationYaw;
            entity.rotationYaw = MathHelper.wrapDegrees(entity.rotationYaw - 2f);
        }
    }

    public static class FlappyPose extends ParrotPose {
        @Override public void tick(PonderScene scene, EntityParrot entity, Vec3d location) {
            entity.setNoGravity(true);
            entity.motionY = Math.sin(scene.getCurrentTime() * .35) * .015;
        }
    }

    public abstract static class FaceVecPose extends ParrotPose {
        protected abstract Vec3d getFacedVec(PonderScene scene);

        @Override public void tick(PonderScene scene, EntityParrot entity, Vec3d location) {
            Vec3d target = getFacedVec(scene);
            Vec3d eye = location.add(0, entity.getEyeHeight(), 0);
            double dx = target.x - eye.x;
            double dy = target.y - eye.y;
            double dz = target.z - eye.z;
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            float pitch = MathHelper.wrapDegrees((float) -Math.toDegrees(Math.atan2(dy, horizontal)));
            float yaw = MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(dz, dx)) - 90f);
            entity.rotationPitch += MathHelper.wrapDegrees(pitch - entity.rotationPitch) * .4f;
            entity.rotationYaw += MathHelper.wrapDegrees(yaw - entity.rotationYaw) * .4f;
        }
    }

    public static class FacePointOfInterestPose extends FaceVecPose {
        @Override protected Vec3d getFacedVec(PonderScene scene) { return scene.getPointOfInterest(); }
    }

    public static class FaceCursorPose extends FaceVecPose {
        @Override protected Vec3d getFacedVec(PonderScene scene) { return scene.getCursorPosition(); }
    }
}
