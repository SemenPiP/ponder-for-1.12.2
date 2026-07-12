package net.createmod.catnip.outliner;

import javax.annotation.Nullable;

import net.createmod.catnip.render.BindableTexture;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;

public abstract class Outline {
    protected final OutlineParams params = new OutlineParams();

    public OutlineParams getParams() { return params; }
    public void tick() {}
    public abstract void render(Vec3d camera, float partialTicks);

    public static class OutlineParams {
        private int color = 0xffffffff;
        private float alpha = 1f;
        private float lineWidth = 1f / 32f;
        private int lightmap = 0x00f000f0;
        private boolean fadeLineWidth = true;
        private boolean disableCull;
        @Nullable private BindableTexture faceTexture;
        @Nullable private BindableTexture highlightedFaceTexture;
        @Nullable private EnumFacing highlightedFace;

        public OutlineParams colored(int color) {
            this.color = color >>> 24 == 0 ? 0xff000000 | color : color;
            return this;
        }
        public OutlineParams alpha(float alpha) { this.alpha = clamp(alpha); return this; }
        public OutlineParams lightmap(int lightmap) { this.lightmap = lightmap; return this; }
        public OutlineParams lineWidth(float width) { lineWidth = Math.max(0, width); return this; }
        public OutlineParams fadeLineWidth(boolean fade) { fadeLineWidth = fade; return this; }
        public OutlineParams withFaceTexture(@Nullable BindableTexture texture) { faceTexture = texture; return this; }
        public OutlineParams withFaceTextures(@Nullable BindableTexture texture,
                                              @Nullable BindableTexture highlight) {
            faceTexture = texture;
            highlightedFaceTexture = highlight;
            return this;
        }
        public OutlineParams clearTextures() { return withFaceTextures(null, null); }
        public OutlineParams highlightFace(@Nullable EnumFacing face) { highlightedFace = face; return this; }
        public OutlineParams disableCull() { disableCull = true; return this; }
        public OutlineParams disableLineNormals() { return this; }

        public int getColor() { return color; }
        public float getAlpha() { return alpha; }
        public float getLineWidth() { return fadeLineWidth ? lineWidth * alpha : lineWidth; }
        public int getLightmap() { return lightmap; }
        public boolean isCullDisabled() { return disableCull; }
        @Nullable public BindableTexture getFaceTexture() { return faceTexture; }
        @Nullable public BindableTexture getHighlightedFaceTexture() { return highlightedFaceTexture; }
        @Nullable public EnumFacing getHighlightedFace() { return highlightedFace; }

        private static float clamp(float value) { return Math.max(0, Math.min(1, value)); }
    }
}
