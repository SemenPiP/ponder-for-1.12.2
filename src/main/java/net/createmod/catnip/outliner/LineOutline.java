package net.createmod.catnip.outliner;

import org.lwjgl.opengl.GL11;

import net.createmod.catnip.render.GlStateGuard;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.Vec3d;

public class LineOutline extends Outline {
    protected Vec3d start = Vec3d.ZERO;
    protected Vec3d end = Vec3d.ZERO;

    public LineOutline() {}
    public LineOutline(Vec3d start, Vec3d end) { set(start, end); }
    public LineOutline set(Vec3d start, Vec3d end) { this.start = start; this.end = end; return this; }

    @Override
    public void render(Vec3d camera, float partialTicks) {
        int color = params.getColor();
        int alpha = Math.round((color >>> 24) * params.getAlpha());
        try (GlStateGuard ignored = GlStateGuard.capture()) {
            GlStateManager.disableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.disableLighting();
            GlStateManager.depthMask(false);
            GL11.glLineWidth(Math.max(1, params.getLineWidth() * 16));
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            vertex(buffer, start.subtract(camera), color, alpha);
            vertex(buffer, end.subtract(camera), color, alpha);
            tessellator.draw();
        }
    }

    protected static void vertex(BufferBuilder buffer, Vec3d pos, int color, int alpha) {
        buffer.pos(pos.x, pos.y, pos.z).color(color >> 16 & 255, color >> 8 & 255,
            color & 255, alpha).endVertex();
    }

    public static class EndChasingLineOutline extends LineOutline {
        private final boolean lockStart;
        private float progress = 1;
        public EndChasingLineOutline(boolean lockStart) { this.lockStart = lockStart; }
        public EndChasingLineOutline setProgress(float progress) {
            this.progress = Math.max(0, Math.min(1, progress));
            return this;
        }
        @Override
        public void render(Vec3d camera, float partialTicks) {
            Vec3d originalStart = start;
            Vec3d originalEnd = end;
            Vec3d delta = end.subtract(start);
            if (lockStart) end = start.add(delta.scale(progress));
            else start = end.subtract(delta.scale(progress));
            super.render(camera, partialTicks);
            start = originalStart;
            end = originalEnd;
        }
    }
}
