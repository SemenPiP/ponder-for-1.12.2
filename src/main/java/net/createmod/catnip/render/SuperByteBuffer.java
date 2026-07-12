package net.createmod.catnip.render;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import net.createmod.catnip.theme.Color;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Reusable transformed CPU mesh for Minecraft 1.12's fixed rendering pipeline.
 * The destination {@link BufferBuilder} must already be drawing with a block-compatible format.
 */
@SideOnly(Side.CLIENT)
public class SuperByteBuffer {
    @Nullable
    protected TemplateMesh mesh;
    private final PoseStack transforms = new PoseStack();
    private boolean colorOverride;
    private int color = 0xffffffff;
    private boolean lightOverride;
    private int light;
    private boolean disableDiffuse;
    @Nullable
    private World lightWorld;
    @Nullable
    private Matrix4f lightTransform;
    @Nullable
    private SpriteShiftEntry spriteShift;
    private boolean deleted;

    public SuperByteBuffer(TemplateMesh mesh) {
        if (mesh == null) {
            throw new IllegalArgumentException("mesh");
        }
        this.mesh = mesh;
    }

    public SuperByteBuffer reset() {
        ensureAlive();
        transforms.clear();
        colorOverride = false;
        color = 0xffffffff;
        lightOverride = false;
        light = 0;
        disableDiffuse = false;
        lightWorld = null;
        lightTransform = null;
        spriteShift = null;
        return this;
    }

    public SuperByteBuffer pushPose() { ensureAlive(); transforms.pushPose(); return this; }
    public SuperByteBuffer popPose() { ensureAlive(); transforms.popPose(); return this; }
    public SuperByteBuffer translate(double x, double y, double z) { ensureAlive(); transforms.translate(x, y, z); return this; }
    public SuperByteBuffer scale(float x, float y, float z) { ensureAlive(); transforms.scale(x, y, z); return this; }
    public SuperByteBuffer rotate(float radians, float x, float y, float z) { ensureAlive(); transforms.rotate(radians, x, y, z); return this; }
    public SuperByteBuffer transform(Matrix4f matrix) { ensureAlive(); transforms.mulPose(matrix); return this; }
    public PoseStack getTransforms() { ensureAlive(); return transforms; }
    public boolean isEmpty() { return deleted || mesh == null || mesh.isEmpty(); }
    public boolean isDeleted() { return deleted; }

    /** Applies an opaque {@code 0xRRGGBB} color override. */
    public SuperByteBuffer color(int rgb) {
        ensureAlive();
        return setColorOverride(0xff000000 | rgb & 0x00ffffff);
    }

    public SuperByteBuffer color(int red, int green, int blue, int alpha) {
        ensureAlive();
        return setColorOverride((alpha & 255) << 24 | (red & 255) << 16
            | (green & 255) << 8 | blue & 255);
    }

    public SuperByteBuffer color(Color color) {
        if (color == null) throw new IllegalArgumentException("color");
        return color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    public SuperByteBuffer color(float red, float green, float blue, float alpha) {
        return color(Math.round(red * 255), Math.round(green * 255), Math.round(blue * 255),
            Math.round(alpha * 255));
    }

    /** Removes the face-direction darkening baked into block-model vertex colors. */
    public SuperByteBuffer disableDiffuse() {
        ensureAlive();
        disableDiffuse = true;
        return this;
    }

    /** Samples 1.12 block/sky light at the locally transformed position of every vertex. */
    public SuperByteBuffer useLevelLight(World world) {
        return useLevelLight(world, null);
    }

    /**
     * Samples level light after applying local transforms and then {@code additionalTransform}.
     * This mirrors the modern API without depending on JOML.
     */
    public SuperByteBuffer useLevelLight(World world, @Nullable Matrix4f additionalTransform) {
        ensureAlive();
        if (world == null) throw new IllegalArgumentException("world");
        lightWorld = world;
        lightTransform = additionalTransform == null ? null : new Matrix4f(additionalTransform);
        return this;
    }

    public SuperByteBuffer shiftUV(SpriteShiftEntry entry) {
        ensureAlive();
        spriteShift = entry;
        return this;
    }

    public SuperByteBuffer shiftUVScrolling(SpriteShiftEntry entry, float scrollV) {
        return shiftUVScrolling(entry, 0, scrollV);
    }

    public SuperByteBuffer shiftUVScrolling(SpriteShiftEntry entry, float scrollU, float scrollV) {
        ensureAlive();
        spriteShift = entry == null ? null : entry.scrolled(scrollU, scrollV);
        return this;
    }

    public SuperByteBuffer rotate(EnumFacing.Axis axis, float radians) {
        if (axis == null) throw new IllegalArgumentException("axis");
        return rotate(radians, axis == EnumFacing.Axis.X ? 1 : 0,
            axis == EnumFacing.Axis.Y ? 1 : 0, axis == EnumFacing.Axis.Z ? 1 : 0);
    }

    /** Releases this buffer's reference to its CPU-side vertex data. */
    public void delete() {
        if (deleted) return;
        deleted = true;
        mesh = null;
        transforms.clear();
        lightWorld = null;
        lightTransform = null;
        spriteShift = null;
    }

    public SuperByteBuffer light(int packedLight) {
        ensureAlive();
        lightOverride = true;
        light = packedLight;
        return this;
    }

    public void renderInto(BufferBuilder target) {
        ensureAlive();
        if (target == null) throw new IllegalArgumentException("target");
        TemplateMesh activeMesh = mesh;
        if (activeMesh == null || activeMesh.isEmpty()) return;

        Matrix4f pose = transforms.last().pose();
        Matrix3f normalMatrix = transforms.last().normal();
        Map<BlockPos, Integer> lightCache = lightWorld == null
            ? null : new HashMap<BlockPos, Integer>();

        for (int i = 0; i < activeMesh.vertexCount(); i++) {
            float originalX = activeMesh.x(i);
            float originalY = activeMesh.y(i);
            float originalZ = activeMesh.z(i);
            Point3f point = new Point3f(originalX, originalY, originalZ);
            pose.transform(point);

            int packedNormal = activeMesh.normal(i);
            Vector3f sourceNormal = new Vector3f(unpackNormal(packedNormal),
                unpackNormal(packedNormal >> 8), unpackNormal(packedNormal >> 16));
            Vector3f normal = new Vector3f(sourceNormal);
            normalMatrix.transform(normal);
            if (normal.lengthSquared() > 0) normal.normalize();

            int vertexColor = colorOverride ? color : activeMesh.color(i);
            if (shouldApplyDiffuse(i)) {
                float outputDiffuse = disableDiffuse ? 1f : diffuse(normal);
                if (!colorOverride) {
                    float bakedDiffuse = diffuse(sourceNormal);
                    outputDiffuse /= Math.max(.001f, bakedDiffuse);
                }
                vertexColor = scaleRgb(vertexColor, outputDiffuse);
            }

            int packedLight = activeMesh.light(i);
            if (lightOverride) packedLight = maxLight(packedLight, light);
            if (lightWorld != null) {
                Point3f lightPoint = new Point3f(
                    ((originalX - .5f) * 15f / 16f) + .5f,
                    ((originalY - .5f) * 15f / 16f) + .5f,
                    ((originalZ - .5f) * 15f / 16f) + .5f);
                pose.transform(lightPoint);
                if (lightTransform != null) lightTransform.transform(lightPoint);
                BlockPos lightPos = new BlockPos(MathHelper.floor(lightPoint.x),
                    MathHelper.floor(lightPoint.y), MathHelper.floor(lightPoint.z));
                Integer sampled = lightCache.get(lightPos);
                if (sampled == null) {
                    sampled = lightWorld.getCombinedLight(lightPos, 0);
                    lightCache.put(lightPos.toImmutable(), sampled);
                }
                packedLight = maxLight(packedLight, sampled);
            }

            float u = activeMesh.u(i);
            float v = activeMesh.v(i);
            if (spriteShift != null) {
                float[] shifted = spriteShift.shift(u, v);
                u = shifted[0];
                v = shifted[1];
            }

            target.pos(point.x, point.y, point.z)
                .color(vertexColor >> 16 & 255, vertexColor >> 8 & 255, vertexColor & 255,
                    vertexColor >>> 24)
                .tex(u, v)
                .lightmap(packedLight & 0xffff, packedLight >>> 16 & 0xffff)
                .normal(normal.x, normal.y, normal.z)
                .endVertex();
        }
    }

    /** Subclasses use this hook to preserve per-quad shade transitions. */
    protected boolean shouldApplyDiffuse(int vertexIndex) {
        return true;
    }

    public static int maxLight(int first, int second) {
        int block = Math.max(first & 0xffff, second & 0xffff);
        int sky = Math.max(first >>> 16 & 0xffff, second >>> 16 & 0xffff);
        return block | sky << 16;
    }

    private static int scaleRgb(int argb, float factor) {
        int alpha = argb >>> 24;
        int red = clampColor(Math.round((argb >> 16 & 255) * factor));
        int green = clampColor(Math.round((argb >> 8 & 255) * factor));
        int blue = clampColor(Math.round((argb & 255) * factor));
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private static int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }

    /** Matches the four directional brightness constants used by BlockModelRenderer. */
    private static float diffuse(Vector3f normal) {
        float x2 = normal.x * normal.x;
        float y2 = normal.y * normal.y;
        float z2 = normal.z * normal.z;
        float sum = x2 + y2 + z2;
        if (sum < 1e-6f) return 1f;
        float yLight = normal.y < 0 ? .5f : 1f;
        return (x2 * .6f + y2 * yLight + z2 * .8f) / sum;
    }

    private static float unpackNormal(int packed) {
        return (byte) packed / 127f;
    }

    private SuperByteBuffer setColorOverride(int argb) {
        colorOverride = true;
        color = argb;
        return this;
    }

    private void ensureAlive() {
        if (deleted) throw new IllegalStateException("SuperByteBuffer has been deleted");
    }
}
