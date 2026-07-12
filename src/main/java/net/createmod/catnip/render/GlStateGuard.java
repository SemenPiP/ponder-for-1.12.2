package net.createmod.catnip.render;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Saves fixed-pipeline state and resynchronizes Minecraft's GL state cache on close. */
@SideOnly(Side.CLIENT)
public final class GlStateGuard implements AutoCloseable {
    private static final int[] TEXTURE_UNITS = {GL13.GL_TEXTURE0, GL13.GL_TEXTURE1};
    private static final int[] TEX_GEN_COORDS = {GL11.GL_S, GL11.GL_T, GL11.GL_R, GL11.GL_Q};
    private static final int[] TEX_GEN_CAPABILITIES = {
        GL11.GL_TEXTURE_GEN_S, GL11.GL_TEXTURE_GEN_T, GL11.GL_TEXTURE_GEN_R, GL11.GL_TEXTURE_GEN_Q
    };
    private static final GlStateManager.TexGen[] TEX_GEN_TYPES = {
        GlStateManager.TexGen.S, GlStateManager.TexGen.T,
        GlStateManager.TexGen.R, GlStateManager.TexGen.Q
    };

    private final int matrixMode;
    private final int activeTexture;
    private final int clientActiveTexture;
    private final int modelViewStackDepth;
    private final int projectionStackDepth;
    private final FloatBuffer modelViewMatrix = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer projectionMatrix = BufferUtils.createFloatBuffer(16);
    private final int[] textureStackDepths = new int[TEXTURE_UNITS.length];
    private final FloatBuffer[] textureMatrices = new FloatBuffer[TEXTURE_UNITS.length];
    private final int[] boundTextures = new int[TEXTURE_UNITS.length];
    private final boolean[] textureEnabled = new boolean[TEXTURE_UNITS.length];
    private final boolean alphaEnabled;
    private final int alphaFunction;
    private final float alphaReference;
    private final boolean depthEnabled;
    private final boolean depthMask;
    private final int depthFunction;
    private final boolean blendEnabled;
    private final int blendSourceRgb;
    private final int blendDestinationRgb;
    private final int blendSourceAlpha;
    private final int blendDestinationAlpha;
    private final boolean lightingEnabled;
    private final boolean[] lightsEnabled = new boolean[8];
    private final boolean colorMaterialEnabled;
    private final int colorMaterialFace;
    private final int colorMaterialMode;
    private final boolean cullEnabled;
    private final int cullFace;
    private final boolean polygonOffsetFillEnabled;
    private final boolean polygonOffsetLineEnabled;
    private final float polygonOffsetFactor;
    private final float polygonOffsetUnits;
    private final boolean colorLogicEnabled;
    private final int colorLogicOperation;
    private final boolean[] texGenEnabled = new boolean[TEX_GEN_TYPES.length];
    private final int[] texGenModes = new int[TEX_GEN_TYPES.length];
    private final boolean fogEnabled;
    private final int fogMode;
    private final float fogDensity;
    private final float fogStart;
    private final float fogEnd;
    private final boolean normalizeEnabled;
    private final boolean rescaleNormalEnabled;
    private final int shadeModel;
    private final float colorRed;
    private final float colorGreen;
    private final float colorBlue;
    private final float colorAlpha;
    private final boolean colorMaskRed;
    private final boolean colorMaskGreen;
    private final boolean colorMaskBlue;
    private final boolean colorMaskAlpha;
    private final double clearDepth;
    private final float clearColorRed;
    private final float clearColorGreen;
    private final float clearColorBlue;
    private final float clearColorAlpha;
    private boolean closed;

    private GlStateGuard() {
        matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        clientActiveTexture = GL11.glGetInteger(GL13.GL_CLIENT_ACTIVE_TEXTURE);
        alphaEnabled = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        alphaFunction = GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);
        alphaReference = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
        depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        depthFunction = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        blendSourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        blendDestinationRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        blendSourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        blendDestinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        lightingEnabled = GL11.glIsEnabled(GL11.GL_LIGHTING);
        for (int i = 0; i < lightsEnabled.length; i++)
            lightsEnabled[i] = GL11.glIsEnabled(GL11.GL_LIGHT0 + i);
        colorMaterialEnabled = GL11.glIsEnabled(GL11.GL_COLOR_MATERIAL);
        colorMaterialFace = GL11.glGetInteger(GL11.GL_COLOR_MATERIAL_FACE);
        colorMaterialMode = GL11.glGetInteger(GL11.GL_COLOR_MATERIAL_PARAMETER);
        cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        cullFace = GL11.glGetInteger(GL11.GL_CULL_FACE_MODE);
        polygonOffsetFillEnabled = GL11.glIsEnabled(GL11.GL_POLYGON_OFFSET_FILL);
        polygonOffsetLineEnabled = GL11.glIsEnabled(GL11.GL_POLYGON_OFFSET_LINE);
        polygonOffsetFactor = GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_FACTOR);
        polygonOffsetUnits = GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_UNITS);
        colorLogicEnabled = GL11.glIsEnabled(GL11.GL_COLOR_LOGIC_OP);
        colorLogicOperation = GL11.glGetInteger(GL11.GL_LOGIC_OP_MODE);
        for (int i = 0; i < TEX_GEN_TYPES.length; i++) {
            texGenEnabled[i] = GL11.glIsEnabled(TEX_GEN_CAPABILITIES[i]);
            texGenModes[i] = GL11.glGetTexGeni(TEX_GEN_COORDS[i], GL11.GL_TEXTURE_GEN_MODE);
        }
        fogEnabled = GL11.glIsEnabled(GL11.GL_FOG);
        fogMode = GL11.glGetInteger(GL11.GL_FOG_MODE);
        fogDensity = GL11.glGetFloat(GL11.GL_FOG_DENSITY);
        fogStart = GL11.glGetFloat(GL11.GL_FOG_START);
        fogEnd = GL11.glGetFloat(GL11.GL_FOG_END);
        normalizeEnabled = GL11.glIsEnabled(GL11.GL_NORMALIZE);
        rescaleNormalEnabled = GL11.glIsEnabled(GL12_RESCALE_NORMAL);
        shadeModel = GL11.glGetInteger(GL11.GL_SHADE_MODEL);
        FloatBuffer color = BufferUtils.createFloatBuffer(4);
        GL11.glGetFloat(GL11.GL_CURRENT_COLOR, color);
        colorRed = color.get(0);
        colorGreen = color.get(1);
        colorBlue = color.get(2);
        colorAlpha = color.get(3);
        ByteBuffer colorMask = BufferUtils.createByteBuffer(4);
        GL11.glGetBoolean(GL11.GL_COLOR_WRITEMASK, colorMask);
        colorMaskRed = colorMask.get(0) != 0;
        colorMaskGreen = colorMask.get(1) != 0;
        colorMaskBlue = colorMask.get(2) != 0;
        colorMaskAlpha = colorMask.get(3) != 0;
        clearDepth = GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);
        FloatBuffer clearColor = BufferUtils.createFloatBuffer(4);
        GL11.glGetFloat(GL11.GL_COLOR_CLEAR_VALUE, clearColor);
        clearColorRed = clearColor.get(0);
        clearColorGreen = clearColor.get(1);
        clearColorBlue = clearColor.get(2);
        clearColorAlpha = clearColor.get(3);

        modelViewStackDepth = captureMatrix(GL11.GL_MODELVIEW, GL11.GL_MODELVIEW_STACK_DEPTH,
            GL11.GL_MODELVIEW_MATRIX, modelViewMatrix);
        projectionStackDepth = captureMatrix(GL11.GL_PROJECTION, GL11.GL_PROJECTION_STACK_DEPTH,
            GL11.GL_PROJECTION_MATRIX, projectionMatrix);
        for (int i = 0; i < TEXTURE_UNITS.length; i++) {
            GL13.glActiveTexture(TEXTURE_UNITS[i]);
            boundTextures[i] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            textureEnabled[i] = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
            textureMatrices[i] = BufferUtils.createFloatBuffer(16);
            textureStackDepths[i] = captureMatrix(GL11.GL_TEXTURE, GL11.GL_TEXTURE_STACK_DEPTH,
                GL11.GL_TEXTURE_MATRIX, textureMatrices[i]);
        }
        GL13.glActiveTexture(activeTexture);
        GL11.glMatrixMode(matrixMode);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);
    }

    public static GlStateGuard capture() {
        return new GlStateGuard();
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        Restoration restoration = new Restoration();

        restoration.attempt(GL11::glPopClientAttrib);
        restoration.attempt(GL11::glPopAttrib);
        restoration.attempt(() -> restoreMatrix(GL11.GL_MODELVIEW, GL11.GL_MODELVIEW_STACK_DEPTH,
            modelViewStackDepth, modelViewMatrix));
        restoration.attempt(() -> restoreMatrix(GL11.GL_PROJECTION, GL11.GL_PROJECTION_STACK_DEPTH,
            projectionStackDepth, projectionMatrix));
        for (int i = 0; i < TEXTURE_UNITS.length; i++) {
            final int texture = i;
            restoration.attempt(() -> {
                GL13.glActiveTexture(TEXTURE_UNITS[texture]);
                restoreMatrix(GL11.GL_TEXTURE, GL11.GL_TEXTURE_STACK_DEPTH,
                    textureStackDepths[texture], textureMatrices[texture]);
            });
        }
        restoration.pair(() -> GL13.glActiveTexture(activeTexture),
            () -> GlStateManager.setActiveTexture(activeTexture));
        restoration.attempt(() -> GlStateManager.matrixMode(matrixMode));

        restoration.pair(() -> restoreCapability(GL11.GL_ALPHA_TEST, alphaEnabled),
            () -> setAlphaEnabled(alphaEnabled));
        restoration.pair(() -> GL11.glAlphaFunc(alphaFunction, alphaReference),
            () -> GlStateManager.alphaFunc(alphaFunction, alphaReference));

        restoration.pair(() -> restoreCapability(GL11.GL_DEPTH_TEST, depthEnabled),
            () -> setDepthEnabled(depthEnabled));
        restoration.pair(() -> GL11.glDepthMask(depthMask), () -> GlStateManager.depthMask(depthMask));
        restoration.pair(() -> GL11.glDepthFunc(depthFunction), () -> GlStateManager.depthFunc(depthFunction));

        restoration.pair(() -> restoreCapability(GL11.GL_BLEND, blendEnabled),
            () -> setBlendEnabled(blendEnabled));
        restoration.pair(() -> GL14.glBlendFuncSeparate(blendSourceRgb, blendDestinationRgb,
                blendSourceAlpha, blendDestinationAlpha),
            () -> GlStateManager.tryBlendFuncSeparate(blendSourceRgb, blendDestinationRgb,
                blendSourceAlpha, blendDestinationAlpha));

        restoration.pair(() -> restoreCapability(GL11.GL_LIGHTING, lightingEnabled),
            () -> setLightingEnabled(lightingEnabled));
        for (int i = 0; i < lightsEnabled.length; i++) {
            final int light = i;
            restoration.pair(() -> restoreCapability(GL11.GL_LIGHT0 + light, lightsEnabled[light]),
                () -> setLightEnabled(light, lightsEnabled[light]));
        }
        restoration.pair(() -> restoreCapability(GL11.GL_COLOR_MATERIAL, colorMaterialEnabled),
            () -> setColorMaterialEnabled(colorMaterialEnabled));
        restoration.pair(() -> GL11.glColorMaterial(colorMaterialFace, colorMaterialMode),
            () -> GlStateManager.colorMaterial(colorMaterialFace, colorMaterialMode));

        restoration.pair(() -> restoreCapability(GL11.GL_CULL_FACE, cullEnabled),
            () -> setCullEnabled(cullEnabled));
        restoration.pair(() -> GL11.glCullFace(cullFace),
            () -> GlStateManager.cullFace(toCullFace(cullFace)));

        restoration.pair(() -> restoreCapability(GL11.GL_POLYGON_OFFSET_FILL, polygonOffsetFillEnabled),
            () -> setPolygonOffsetEnabled(polygonOffsetFillEnabled));
        restoration.attempt(() -> restoreCapability(GL11.GL_POLYGON_OFFSET_LINE, polygonOffsetLineEnabled));
        restoration.pair(() -> GL11.glPolygonOffset(polygonOffsetFactor, polygonOffsetUnits),
            () -> GlStateManager.doPolygonOffset(polygonOffsetFactor, polygonOffsetUnits));

        restoration.pair(() -> restoreCapability(GL11.GL_COLOR_LOGIC_OP, colorLogicEnabled),
            () -> setColorLogicEnabled(colorLogicEnabled));
        restoration.pair(() -> GL11.glLogicOp(colorLogicOperation),
            () -> GlStateManager.colorLogicOp(colorLogicOperation));
        for (int i = 0; i < TEX_GEN_TYPES.length; i++) {
            final int texGen = i;
            restoration.pair(() -> restoreCapability(TEX_GEN_CAPABILITIES[texGen], texGenEnabled[texGen]),
                () -> setTexGenEnabled(TEX_GEN_TYPES[texGen], texGenEnabled[texGen]));
            restoration.pair(() -> GL11.glTexGeni(TEX_GEN_COORDS[texGen], GL11.GL_TEXTURE_GEN_MODE,
                    texGenModes[texGen]),
                () -> GlStateManager.texGen(TEX_GEN_TYPES[texGen], texGenModes[texGen]));
        }

        restoration.pair(() -> restoreCapability(GL11.GL_FOG, fogEnabled),
            () -> setFogEnabled(fogEnabled));
        restoration.pair(() -> GL11.glFogi(GL11.GL_FOG_MODE, fogMode),
            () -> GlStateManager.setFog(toFogMode(fogMode)));
        restoration.pair(() -> GL11.glFogf(GL11.GL_FOG_DENSITY, fogDensity),
            () -> GlStateManager.setFogDensity(fogDensity));
        restoration.pair(() -> GL11.glFogf(GL11.GL_FOG_START, fogStart),
            () -> GlStateManager.setFogStart(fogStart));
        restoration.pair(() -> GL11.glFogf(GL11.GL_FOG_END, fogEnd),
            () -> GlStateManager.setFogEnd(fogEnd));

        restoration.pair(() -> restoreCapability(GL11.GL_NORMALIZE, normalizeEnabled),
            () -> setNormalizeEnabled(normalizeEnabled));
        restoration.pair(() -> restoreCapability(GL12_RESCALE_NORMAL, rescaleNormalEnabled),
            () -> setRescaleNormalEnabled(rescaleNormalEnabled));
        restoration.pair(() -> GL11.glShadeModel(shadeModel), () -> GlStateManager.shadeModel(shadeModel));
        restoration.pair(() -> GL11.glColor4f(colorRed, colorGreen, colorBlue, colorAlpha),
            () -> GlStateManager.color(colorRed, colorGreen, colorBlue, colorAlpha));
        restoration.pair(() -> GL11.glColorMask(colorMaskRed, colorMaskGreen, colorMaskBlue, colorMaskAlpha),
            () -> GlStateManager.colorMask(colorMaskRed, colorMaskGreen, colorMaskBlue, colorMaskAlpha));
        restoration.pair(() -> GL11.glClearDepth(clearDepth), () -> GlStateManager.clearDepth(clearDepth));
        restoration.pair(() -> GL11.glClearColor(clearColorRed, clearColorGreen, clearColorBlue, clearColorAlpha),
            () -> GlStateManager.clearColor(clearColorRed, clearColorGreen, clearColorBlue, clearColorAlpha));

        for (int i = 0; i < TEXTURE_UNITS.length; i++) {
            final int texture = i;
            restoration.pair(() -> GL13.glActiveTexture(TEXTURE_UNITS[texture]),
                () -> GlStateManager.setActiveTexture(TEXTURE_UNITS[texture]));
            restoration.pair(() -> GL11.glBindTexture(GL11.GL_TEXTURE_2D, boundTextures[texture]),
                () -> GlStateManager.bindTexture(boundTextures[texture]));
            restoration.pair(() -> restoreCapability(GL11.GL_TEXTURE_2D, textureEnabled[texture]),
                () -> setTexture2DEnabled(textureEnabled[texture]));
        }
        restoration.pair(() -> GL13.glActiveTexture(activeTexture),
            () -> GlStateManager.setActiveTexture(activeTexture));
        restoration.attempt(() -> GL13.glClientActiveTexture(clientActiveTexture));
        restoration.attempt(() -> GlStateManager.matrixMode(matrixMode));
        restoration.finish();
    }

    private static final int GL12_RESCALE_NORMAL = 0x803A;

    private static void setAlphaEnabled(boolean enabled) {
        if (enabled) GlStateManager.enableAlpha(); else GlStateManager.disableAlpha();
    }

    private static void setDepthEnabled(boolean enabled) {
        if (enabled) GlStateManager.enableDepth(); else GlStateManager.disableDepth();
    }

    private static void setBlendEnabled(boolean enabled) {
        if (enabled) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
    }

    private static void setLightingEnabled(boolean enabled) {
        if (enabled) GlStateManager.enableLighting(); else GlStateManager.disableLighting();
    }

    private static void setLightEnabled(int light, boolean enabled) {
        if (enabled) GlStateManager.enableLight(light); else GlStateManager.disableLight(light);
    }

    private static void setColorMaterialEnabled(boolean enabled) {
        if (enabled) GlStateManager.enableColorMaterial(); else GlStateManager.disableColorMaterial();
    }

    private static void setCullEnabled(boolean enabled) {
        if (enabled) GlStateManager.enableCull(); else GlStateManager.disableCull();
    }

    private static void setPolygonOffsetEnabled(boolean enabled) {
        if (enabled) GlStateManager.enablePolygonOffset(); else GlStateManager.disablePolygonOffset();
    }

    private static void setColorLogicEnabled(boolean enabled) {
        if (enabled) GlStateManager.enableColorLogic(); else GlStateManager.disableColorLogic();
    }

    private static void setTexGenEnabled(GlStateManager.TexGen texGen, boolean enabled) {
        if (enabled) GlStateManager.enableTexGenCoord(texGen); else GlStateManager.disableTexGenCoord(texGen);
    }

    private static void setFogEnabled(boolean enabled) {
        if (enabled) GlStateManager.enableFog(); else GlStateManager.disableFog();
    }

    private static void setNormalizeEnabled(boolean enabled) {
        if (enabled) GlStateManager.enableNormalize(); else GlStateManager.disableNormalize();
    }

    private static void setRescaleNormalEnabled(boolean enabled) {
        if (enabled) GlStateManager.enableRescaleNormal(); else GlStateManager.disableRescaleNormal();
    }

    private static void setTexture2DEnabled(boolean enabled) {
        if (enabled) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D();
    }

    private static GlStateManager.CullFace toCullFace(int face) {
        if (face == GL11.GL_FRONT) return GlStateManager.CullFace.FRONT;
        if (face == GL11.GL_FRONT_AND_BACK) return GlStateManager.CullFace.FRONT_AND_BACK;
        return GlStateManager.CullFace.BACK;
    }

    private static GlStateManager.FogMode toFogMode(int mode) {
        if (mode == GL11.GL_LINEAR) return GlStateManager.FogMode.LINEAR;
        if (mode == GL11.GL_EXP2) return GlStateManager.FogMode.EXP2;
        return GlStateManager.FogMode.EXP;
    }

    private static void restoreCapability(int capability, boolean enabled) {
        if (enabled) GL11.glEnable(capability); else GL11.glDisable(capability);
    }

    private static int captureMatrix(int mode, int depthName, int matrixName, FloatBuffer target) {
        GL11.glMatrixMode(mode);
        target.clear();
        GL11.glGetFloat(matrixName, target);
        target.rewind();
        return GL11.glGetInteger(depthName);
    }

    private static void restoreMatrix(int mode, int depthName, int targetDepth, FloatBuffer matrix) {
        GL11.glMatrixMode(mode);
        int currentDepth = GL11.glGetInteger(depthName);
        while (currentDepth > targetDepth) {
            GL11.glPopMatrix();
            currentDepth--;
        }
        while (currentDepth < targetDepth) {
            GL11.glPushMatrix();
            currentDepth++;
        }
        matrix.rewind();
        GL11.glLoadMatrix(matrix);
    }

    @FunctionalInterface
    interface RestoreOperation {
        void run();
    }

    /** Runs every cleanup step, retaining the first failure and suppressing later failures. */
    static final class Restoration {
        private Throwable failure;

        void attempt(RestoreOperation operation) {
            try {
                operation.run();
            } catch (Throwable thrown) {
                if (failure == null) failure = thrown;
                else if (failure != thrown) failure.addSuppressed(thrown);
            }
        }

        void pair(RestoreOperation driverState, RestoreOperation minecraftCache) {
            attempt(driverState);
            attempt(minecraftCache);
        }

        void finish() {
            if (failure == null) return;
            if (failure instanceof RuntimeException) throw (RuntimeException) failure;
            if (failure instanceof Error) throw (Error) failure;
            throw new IllegalStateException("Unexpected checked exception while restoring OpenGL state", failure);
        }
    }
}
