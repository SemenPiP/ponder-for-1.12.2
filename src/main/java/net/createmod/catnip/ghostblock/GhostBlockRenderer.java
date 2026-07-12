package net.createmod.catnip.ghostblock;

import net.createmod.catnip.render.GlStateGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.math.Vec3d;

public abstract class GhostBlockRenderer {
    private static final GhostBlockRenderer STANDARD = new FixedPipelineRenderer(false);
    private static final GhostBlockRenderer TRANSPARENT = new FixedPipelineRenderer(true);

    public static GhostBlockRenderer standard() { return STANDARD; }
    public static GhostBlockRenderer transparent() { return TRANSPARENT; }
    public abstract void render(Vec3d camera, GhostBlockParams params);

    private static final class FixedPipelineRenderer extends GhostBlockRenderer {
        private final boolean transparent;

        private FixedPipelineRenderer(boolean transparent) { this.transparent = transparent; }

        @Override
        public void render(Vec3d camera, GhostBlockParams params) {
            try (GlStateGuard ignored = GlStateGuard.capture()) {
                boolean matrixPushed = false;
                try {
                    GlStateManager.pushMatrix();
                    matrixPushed = true;
                    GlStateManager.translate(params.getPos().getX() - camera.x,
                        params.getPos().getY() - camera.y, params.getPos().getZ() - camera.z);
                    if (transparent) {
                        GlStateManager.enableBlend();
                        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                        GlStateManager.depthMask(false);
                        float scale = .85f;
                        GlStateManager.translate(.5f, .5f, .5f);
                        GlStateManager.scale(scale, scale, scale);
                        GlStateManager.translate(-.5f, -.5f, -.5f);
                        GlStateManager.color(1f, 1f, 1f, params.getAlpha() * .75f);
                    }
                    RenderHelper.disableStandardItemLighting();
                    Minecraft.getMinecraft().getBlockRendererDispatcher()
                        .renderBlockBrightness(params.getState(), 1f);
                } finally {
                    if (matrixPushed) GlStateManager.popMatrix();
                }
            }
        }
    }
}
