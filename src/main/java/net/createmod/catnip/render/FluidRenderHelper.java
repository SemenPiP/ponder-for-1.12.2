package net.createmod.catnip.render;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.catnip.platform.services.ModFluidHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

public final class FluidRenderHelper {
    private FluidRenderHelper() {}

    public static void renderFluidBox(FluidStack fluid, float x0, float y0, float z0,
                                      float x1, float y1, float z1, BufferBuilder buffer,
                                      PoseStack pose, int light, boolean bottom,
                                      boolean invertGases) {
        if (fluid == null || fluid.getFluid() == null || fluid.amount <= 0) return;
        if (buffer == null || pose == null) throw new IllegalArgumentException("buffer/pose");

        Fluid type = fluid.getFluid();
        TextureAtlasSprite sprite = Minecraft.getMinecraft()
            .getTextureMapBlocks()
            .getAtlasSprite(type.getStill(fluid).toString());
        @SuppressWarnings("unchecked")
        ModFluidHelper<FluidStack> helper = (ModFluidHelper<FluidStack>) CatnipServices.FLUID_HELPER;
        int color = helper.getColor(fluid);
        int luminosity = Math.max((light >> 4) & 15, helper.getLuminosity(fluid));
        light = (light & 0xf00000) | luminosity << 4;

        pose.pushPose();
        try {
            boolean lighterThanAir = invertGases && helper.isLighterThanAir(fluid);
            applyGasInversion(pose, x0, y0, z0, x1, y1, z1,
                invertGases, lighterThanAir);

            face(buffer, pose, EnumFacing.UP, x0, y1, z0, x1, y1, z1,
                color, light, sprite);
            if (bottom) {
                face(buffer, pose, EnumFacing.DOWN, x0, y0, z1, x1, y0, z0,
                    color, light, sprite);
            }
            face(buffer, pose, EnumFacing.NORTH, x1, y0, z0, x0, y1, z0,
                color, light, sprite);
            face(buffer, pose, EnumFacing.SOUTH, x0, y0, z1, x1, y1, z1,
                color, light, sprite);
            face(buffer, pose, EnumFacing.WEST, x0, y0, z0, x0, y1, z1,
                color, light, sprite);
            face(buffer, pose, EnumFacing.EAST, x1, y0, z1, x1, y1, z0,
                color, light, sprite);
        } finally {
            pose.popPose();
        }
    }

    static void applyGasInversion(PoseStack pose, float x0, float y0, float z0,
                                  float x1, float y1, float z1,
                                  boolean invertGases, boolean lighterThanAir) {
        if (!invertGases || !lighterThanAir) return;

        float centerX = (x0 + x1) * .5f;
        float centerY = (y0 + y1) * .5f;
        float centerZ = (z0 + z1) * .5f;
        pose.translate(centerX, centerY, centerZ);
        pose.rotate((float) Math.PI, 1, 0, 0);
        pose.translate(-centerX, -centerY, -centerZ);
    }

    private static void face(BufferBuilder buffer, PoseStack pose, EnumFacing facing,
                             float ax, float ay, float az, float cx, float cy, float cz,
                             int color, int light, TextureAtlasSprite sprite) {
        emitFace(buffer, pose, facing, ax, ay, az, cx, cy, cz, color, light,
            sprite.getMinU(), sprite.getMinV(), sprite.getMaxU(), sprite.getMaxV());
    }

    static void emitFace(BufferBuilder buffer, PoseStack pose, EnumFacing facing,
                         float ax, float ay, float az, float cx, float cy, float cz,
                         int color, int light, float minU, float minV,
                         float maxU, float maxV) {
        if (facing.getAxis() == EnumFacing.Axis.Y) {
            // Looking from outside the face, UP is counter-clockwise and DOWN is clockwise.
            // The callers provide opposite Z diagonals for those two faces, so this order
            // keeps the geometric normal aligned with the declared facing in both cases.
            vertex(buffer, pose, ax, ay, az, color, light, minU, minV, facing);
            vertex(buffer, pose, ax, ay, cz, color, light, minU, maxV, facing);
            vertex(buffer, pose, cx, cy, cz, color, light, maxU, maxV, facing);
            vertex(buffer, pose, cx, cy, az, color, light, maxU, minV, facing);
            return;
        }

        float bx = cx;
        float by = ay;
        float bz = az;
        float dx = ax;
        float dy = cy;
        float dz = cz;
        if (facing.getAxis() == EnumFacing.Axis.X) {
            bx = ax;
            bz = cz;
            dx = cx;
            dz = az;
        }
        vertex(buffer, pose, ax, ay, az, color, light, minU, minV, facing);
        vertex(buffer, pose, bx, by, bz, color, light, maxU, minV, facing);
        vertex(buffer, pose, cx, cy, cz, color, light, maxU, maxV, facing);
        vertex(buffer, pose, dx, dy, dz, color, light, minU, maxV, facing);
    }

    private static void vertex(BufferBuilder buffer, PoseStack pose,
                               float x, float y, float z, int color, int light,
                               float u, float v, EnumFacing face) {
        Point3f point = new Point3f(x, y, z);
        pose.last().pose().transform(point);
        Vector3f normal = new Vector3f(face.getXOffset(), face.getYOffset(), face.getZOffset());
        pose.last().normal().transform(normal);
        if (normal.lengthSquared() > 0) normal.normalize();
        buffer.pos(point.x, point.y, point.z)
            .color(color >> 16 & 255, color >> 8 & 255, color & 255, color >>> 24)
            .tex(u, v)
            .lightmap(light & 0xffff, light >>> 16 & 0xffff)
            .normal(normal.x, normal.y, normal.z)
            .endVertex();
    }
}
