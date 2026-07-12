package net.createmod.ponder.render;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Captures the exact matrices used for scene rendering so overlays share its projection. */
@SideOnly(Side.CLIENT)
public final class SceneProjection {
    private static volatile Snapshot latest;

    private SceneProjection() {}

    public static void captureCurrent() {
        FloatBuffer model = BufferUtils.createFloatBuffer(16);
        FloatBuffer projection = BufferUtils.createFloatBuffer(16);
        IntBuffer viewport = BufferUtils.createIntBuffer(16);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, model);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
        latest = new Snapshot(model, projection, viewport);
    }

    public static ProjectedPoint project(Vec3d point) {
        Snapshot snapshot = latest;
        if (snapshot == null) return ProjectedPoint.INVISIBLE;
        FloatBuffer result = BufferUtils.createFloatBuffer(3);
        boolean success = GLU.gluProject((float) point.x, (float) point.y, (float) point.z,
            duplicate(snapshot.model), duplicate(snapshot.projection), duplicate(snapshot.viewport), result);
        if (!success) return ProjectedPoint.INVISIBLE;
        Minecraft minecraft = Minecraft.getMinecraft();
        ScaledResolution scaled = new ScaledResolution(minecraft);
        float x = result.get(0) * scaled.getScaledWidth() / minecraft.displayWidth;
        float y = scaled.getScaledHeight() - result.get(1) * scaled.getScaledHeight() / minecraft.displayHeight;
        float depth = result.get(2);
        return new ProjectedPoint(x, y, depth, depth >= 0 && depth <= 1);
    }

    /** Converts a scaled-GUI coordinate and OpenGL depth back into scene coordinates. */
    public static Vec3d unproject(float mouseX, float mouseY, float depth) {
        Snapshot snapshot = latest;
        if (snapshot == null || depth < 0 || depth > 1) return null;
        Minecraft minecraft = Minecraft.getMinecraft();
        ScaledResolution scaled = new ScaledResolution(minecraft);
        float windowX = mouseX * minecraft.displayWidth / Math.max(1f, scaled.getScaledWidth());
        float windowY = minecraft.displayHeight - mouseY * minecraft.displayHeight
            / Math.max(1f, scaled.getScaledHeight()) - 1;
        FloatBuffer result = BufferUtils.createFloatBuffer(3);
        boolean success = GLU.gluUnProject(windowX, windowY, depth,
            duplicate(snapshot.model), duplicate(snapshot.projection), duplicate(snapshot.viewport), result);
        return success ? new Vec3d(result.get(0), result.get(1), result.get(2)) : null;
    }

    /** Returns the exact near/far scene-space segment represented by a GUI pixel. */
    public static ScreenRay screenRay(float mouseX, float mouseY) {
        Vec3d near = unproject(mouseX, mouseY, 0);
        Vec3d far = unproject(mouseX, mouseY, 1);
        return near == null || far == null ? null : new ScreenRay(near, far);
    }

    private static FloatBuffer duplicate(FloatBuffer source) { FloatBuffer copy=source.asReadOnlyBuffer(); copy.rewind(); return copy; }
    private static IntBuffer duplicate(IntBuffer source) { IntBuffer copy=source.asReadOnlyBuffer(); copy.rewind(); return copy; }

    private static final class Snapshot {
        final FloatBuffer model;
        final FloatBuffer projection;
        final IntBuffer viewport;
        Snapshot(FloatBuffer model,FloatBuffer projection,IntBuffer viewport){this.model=model;this.projection=projection;this.viewport=viewport;}
    }

    public static final class ProjectedPoint {
        static final ProjectedPoint INVISIBLE=new ProjectedPoint(0,0,1,false);
        public final float x;
        public final float y;
        public final float depth;
        public final boolean visible;
        ProjectedPoint(float x,float y,float depth,boolean visible){this.x=x;this.y=y;this.depth=depth;this.visible=visible;}
    }

    public static final class ScreenRay {
        public final Vec3d near;
        public final Vec3d far;

        ScreenRay(Vec3d near, Vec3d far) {
            this.near = near;
            this.far = far;
        }
    }
}
