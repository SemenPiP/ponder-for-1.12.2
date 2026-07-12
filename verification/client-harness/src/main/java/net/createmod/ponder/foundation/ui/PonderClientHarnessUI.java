package net.createmod.ponder.foundation.ui;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;

import net.createmod.ponder.api.element.PonderElement;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.AnimatedSceneElementBase;
import net.createmod.ponder.foundation.element.WorldSectionElementImpl;
import net.createmod.ponder.mixin.ParticleManagerAccessor;
import net.createmod.ponder.render.PonderWorldRenderer;
import net.createmod.ponder.render.SectionRenderCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Development-only driver for exercising the real Ponder UI interaction paths. */
public final class PonderClientHarnessUI extends PonderUI {
    private static final Field YAW = field(PonderUI.class, "yaw");
    private static final Field PITCH = field(PonderUI.class, "pitch");
    private static final Field ZOOM = field(PonderUI.class, "zoom");
    private static final Field IDENTIFY_MODE = field(PonderUI.class, "identifyMode");
    private static final Field IDENTIFIED_STACK = field(PonderUI.class, "identifiedStack");
    private static final Field IDENTIFIED_BLOCK = field(PonderUI.class, "identifiedBlock");
    private static final Field RENDERER = field(PonderUI.class, "renderer");
    private static final Field PARTICLE_MANAGERS = field(PonderWorldRenderer.class, "particleManagers");
    private static final Field SECTION_CACHE = field(WorldSectionElementImpl.class, "cache");
    private static final Field SECTION_SELECTION = field(WorldSectionElementImpl.class, "selection");
    private static final Field ELEMENT_FADE = field(AnimatedSceneElementBase.class, "fade");
    private static final Field CACHE_DIRTY = field(SectionRenderCache.class, "dirty");
    private static final Field CACHE_LAYERS = field(SectionRenderCache.class, "layers");
    private static final Field CACHE_TILES = field(SectionRenderCache.class, "tiles");

    private FloatBuffer depthLine = BufferUtils.createFloatBuffer(1);
    private volatile DepthSnapshot depthSnapshot;
    private volatile Throwable depthReadFailure;
    private long depthFrame;
    private GlStateSnapshot pendingGlState;
    private volatile RenderPassSnapshot renderPassSnapshot;
    private volatile Throwable renderPassFailure;
    private long renderPassFrame;

    public static PonderClientHarnessUI create(ResourceLocation component) {
        return new PonderClientHarnessUI(PonderIndex.getSceneAccess().compile(component));
    }

    private PonderClientHarnessUI(List<PonderScene> scenes) {
        super(scenes);
    }

    public void harnessPressKey(int keyCode) throws IOException {
        keyTyped('\0', keyCode);
    }

    public void harnessDrag(int fromX, int fromY, int toX, int toY) throws IOException {
        mouseClicked(fromX, fromY, 0);
        mouseClickMove(toX, toY, 0, 50L);
        mouseReleased(toX, toY, 0);
    }

    /** Applies the same bounds as the mouse-wheel handler without depending on a native event injector. */
    public void harnessZoomBy(float factor) {
        if (!(factor > 0) || Float.isInfinite(factor) || Float.isNaN(factor)) {
            throw new IllegalArgumentException("Zoom factor must be finite and positive");
        }
        float current = getFloat(ZOOM, this);
        setFloat(ZOOM, this, Math.max(.35f, Math.min(3f, current * factor)));
    }

    public CameraState harnessCameraState() {
        return new CameraState(getFloat(YAW, this), getFloat(PITCH, this), getFloat(ZOOM, this));
    }

    public boolean harnessIdentifyMode() {
        return getBoolean(IDENTIFY_MODE, this);
    }

    public ItemStack harnessIdentifiedStack() {
        ItemStack stack = (ItemStack) get(IDENTIFIED_STACK, this);
        return stack == null ? ItemStack.EMPTY : stack.copy();
    }

    public BlockPos harnessIdentifiedBlock() {
        BlockPos pos = (BlockPos) get(IDENTIFIED_BLOCK, this);
        return pos == null ? null : pos.toImmutable();
    }

    public int harnessParticleCount() {
        PonderWorldRenderer renderer = (PonderWorldRenderer) get(RENDERER, this);
        @SuppressWarnings("unchecked")
        Map<Object, ParticleManager> managers = (Map<Object, ParticleManager>) get(PARTICLE_MANAGERS, renderer);
        ParticleManager manager = managers.get(getActiveScene().getWorld());
        if (manager == null) return 0;
        int count = 0;
        for (java.util.ArrayDeque<Particle>[] layer : ((ParticleManagerAccessor) manager).ponder$getParticleLayers()) {
            for (java.util.ArrayDeque<Particle> queue : layer) count += queue.size();
        }
        return count;
    }

    @Override
    void beforeScenePassForTesting() {
        try {
            pendingGlState = GlStateSnapshot.capture();
            renderPassFailure = null;
        } catch (Throwable throwable) {
            pendingGlState = null;
            renderPassFailure = throwable;
        }
    }

    @Override
    void afterScenePassForTesting() {
        long frame = ++renderPassFrame;
        try {
            if (pendingGlState == null) {
                Throwable failure = renderPassFailure;
                throw new IllegalStateException("No pre-render OpenGL state was captured", failure);
            }
            GlStateSnapshot after = GlStateSnapshot.capture();
            PonderScene scene = getActiveScene();
            renderPassSnapshot = new RenderPassSnapshot(frame, scene.getComponent(), scene.getCurrentTick(),
                pendingGlState, after);
            renderPassFailure = null;
        } catch (Throwable throwable) {
            renderPassFailure = throwable;
        } finally {
            pendingGlState = null;
        }
        try {
            depthSnapshot = readDepth(++depthFrame);
            depthReadFailure = null;
        } catch (Throwable throwable) {
            depthReadFailure = throwable;
        }
    }

    public RenderPassSnapshot harnessRenderPassSnapshot() {
        Throwable failure = renderPassFailure;
        if (failure != null) {
            throw new IllegalStateException("Could not inspect the Ponder render pass", failure);
        }
        return renderPassSnapshot;
    }

    public DepthSnapshot harnessDepthSnapshot() {
        Throwable failure = depthReadFailure;
        if (failure != null) {
            throw new IllegalStateException("Could not read the Ponder scene depth buffer", failure);
        }
        return depthSnapshot;
    }

    private DepthSnapshot readDepth(long frame) {
        Minecraft minecraft = Minecraft.getMinecraft();
        int framebufferWidth = minecraft.displayWidth;
        int framebufferHeight = minecraft.displayHeight;
        if (framebufferWidth <= 0 || framebufferHeight <= 0) {
            throw new IllegalStateException("Framebuffer has no drawable area");
        }

        DepthAccumulator depths = new DepthAccumulator();
        int x = Math.max(0, framebufferWidth / 10);
        int y = Math.max(0, framebufferHeight / 5);
        int scanWidth = Math.max(1, framebufferWidth - x * 2);
        int scanHeight = Math.max(1, framebufferHeight - y * 2);
        for (int line = 1; line <= 7; line++) {
            int scanY = y + scanHeight * line / 8;
            readDepthLine(x, scanY, scanWidth, 1, depths);
        }
        for (int line = 1; line <= 7; line++) {
            int scanX = x + scanWidth * line / 8;
            readDepthLine(scanX, y, 1, scanHeight, depths);
        }
        return depths.snapshot(frame, framebufferWidth, framebufferHeight);
    }

    private void readDepthLine(int x, int y, int width, int height, DepthAccumulator depths) {
        int samples = Math.max(1, width * height);
        if (depthLine.capacity() < samples) depthLine = BufferUtils.createFloatBuffer(samples);
        depthLine.clear();
        depthLine.limit(samples);
        GL11.glReadPixels(x, y, width, height, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depthLine);
        for (int i = 0; i < samples; i++) depths.accept(depthLine.get(i));
    }

    public CacheState harnessCacheState() {
        int sections = 0;
        int dirty = 0;
        int clean = 0;
        int cachedTiles = 0;
        EnumSet<BlockRenderLayer> layers = EnumSet.noneOf(BlockRenderLayer.class);
        EnumMap<BlockRenderLayer, Integer> vertices =
            new EnumMap<BlockRenderLayer, Integer>(BlockRenderLayer.class);
        for (PonderElement element : getActiveScene().getElements()) {
            if (!(element instanceof WorldSectionElementImpl)) continue;
            sections++;
            SectionRenderCache cache = (SectionRenderCache) get(SECTION_CACHE, element);
            if (getBoolean(CACHE_DIRTY, cache)) dirty++; else clean++;
            @SuppressWarnings("unchecked")
            Map<BlockRenderLayer, BufferBuilder.State> cachedLayers =
                (Map<BlockRenderLayer, BufferBuilder.State>) get(CACHE_LAYERS, cache);
            layers.addAll(cachedLayers.keySet());
            for (Map.Entry<BlockRenderLayer, BufferBuilder.State> entry : cachedLayers.entrySet()) {
                int count = entry.getValue() == null ? 0 : entry.getValue().getVertexCount();
                Integer previous = vertices.get(entry.getKey());
                vertices.put(entry.getKey(), Integer.valueOf((previous == null ? 0 : previous.intValue()) + count));
            }
            cachedTiles += ((List<?>) get(CACHE_TILES, cache)).size();
        }
        return new CacheState(sections, dirty, clean, cachedTiles, layers, vertices);
    }

    public SectionSnapshot harnessSectionSnapshot() {
        PonderScene scene = getActiveScene();
        Set<BlockPos> visiblePositions = new LinkedHashSet<BlockPos>();
        int sections = 0;
        int temporarySections = 0;
        int visibleSections = 0;
        int partialSections = 0;
        for (PonderElement element : scene.getElements()) {
            if (!(element instanceof WorldSectionElementImpl)) continue;
            sections++;
            if (element != scene.getBaseWorldSection()) temporarySections++;
            if (!element.isVisible()) continue;
            visibleSections++;
            float fade = getFloat(ELEMENT_FADE, element);
            if (fade > .0001f && fade < .9999f) partialSections++;
            Selection selection = (Selection) get(SECTION_SELECTION, element);
            for (BlockPos pos : selection) visiblePositions.add(pos.toImmutable());
        }
        int floorBlocks = 0;
        int upperBlocks = 0;
        for (BlockPos pos : visiblePositions) {
            if (pos.getY() == 0) floorBlocks++;
            else if (pos.getY() > 0) upperBlocks++;
        }
        return new SectionSnapshot(sections, temporarySections, visibleSections, partialSections,
            floorBlocks, upperBlocks, visiblePositions);
    }

    public boolean harnessControlsFit() {
        List<GuiButton> visible = new ArrayList<GuiButton>();
        for (GuiButton button : buttonList) if (button.visible) visible.add(button);
        for (GuiButton button : visible) {
            if (button.x < 0 || button.y < 0 || button.x + button.width > width
                || button.y + button.height > height) return false;
        }
        for (int i = 0; i < visible.size(); i++) {
            GuiButton first = visible.get(i);
            if (first.id == 7) continue;
            for (int j = i + 1; j < visible.size(); j++) {
                GuiButton second = visible.get(j);
                if (second.id == 7) continue;
                if (first.x < second.x + second.width && first.x + first.width > second.x
                    && first.y < second.y + second.height && first.y + first.height > second.y) return false;
            }
        }
        return true;
    }

    private static Field field(Class<?> owner, String name) {
        try {
            Field result = owner.getDeclaredField(name);
            result.setAccessible(true);
            return result;
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static Object get(Field field, Object owner) {
        try {
            return field.get(owner);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Could not inspect Ponder client state", exception);
        }
    }

    private static float getFloat(Field field, Object owner) {
        try {
            return field.getFloat(owner);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Could not inspect Ponder client state", exception);
        }
    }

    private static boolean getBoolean(Field field, Object owner) {
        try {
            return field.getBoolean(owner);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Could not inspect Ponder client state", exception);
        }
    }

    private static void setFloat(Field field, Object owner, float value) {
        try {
            field.setFloat(owner, value);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Could not drive Ponder client state", exception);
        }
    }

    public static final class RenderPassSnapshot {
        public final long frame;
        public final ResourceLocation component;
        public final int sceneTick;
        private final GlStateSnapshot before;
        private final GlStateSnapshot after;

        RenderPassSnapshot(long frame, ResourceLocation component, int sceneTick,
                           GlStateSnapshot before, GlStateSnapshot after) {
            this.frame = frame;
            this.component = component;
            this.sceneTick = sceneTick;
            this.before = before;
            this.after = after;
        }

        public boolean hasNoGlErrors() {
            return before.entryGlError == GL11.GL_NO_ERROR && before.queryGlError == GL11.GL_NO_ERROR
                && after.entryGlError == GL11.GL_NO_ERROR && after.queryGlError == GL11.GL_NO_ERROR;
        }

        public String glErrors() {
            return "before=" + glError(before.entryGlError) + "/" + glError(before.queryGlError)
                + ", after=" + glError(after.entryGlError) + "/" + glError(after.queryGlError);
        }

        public String restorationDifferences() {
            return after.differencesFrom(before);
        }

        @Override public String toString() {
            String differences = restorationDifferences();
            return "frame=" + frame + ", component=" + component + ", tick=" + sceneTick
                + ", glErrors={" + glErrors() + "}, restoration="
                + (differences.isEmpty() ? "exact" : differences);
        }
    }

    private static final class GlStateSnapshot {
        private static final int[] TEXTURE_UNITS = {GL13.GL_TEXTURE0, GL13.GL_TEXTURE1};

        int entryGlError;
        int queryGlError;
        int matrixMode;
        int activeTexture;
        int clientActiveTexture;
        int modelViewStackDepth;
        int projectionStackDepth;
        final int[] textureStackDepths = new int[TEXTURE_UNITS.length];
        float[] modelViewMatrix;
        float[] projectionMatrix;
        final float[][] textureMatrices = new float[TEXTURE_UNITS.length][];
        final int[] boundTextures = new int[TEXTURE_UNITS.length];
        final boolean[] textureEnabled = new boolean[TEXTURE_UNITS.length];
        boolean alphaEnabled;
        int alphaFunction;
        float alphaReference;
        boolean depthEnabled;
        boolean depthMask;
        int depthFunction;
        boolean blendEnabled;
        int blendSourceRgb;
        int blendDestinationRgb;
        int blendSourceAlpha;
        int blendDestinationAlpha;
        boolean lightingEnabled;
        final boolean[] lightsEnabled = new boolean[8];
        boolean colorMaterialEnabled;
        boolean normalizeEnabled;
        boolean rescaleNormalEnabled;
        boolean fogEnabled;
        boolean cullEnabled;
        boolean scissorEnabled;
        boolean polygonOffsetEnabled;
        float polygonOffsetFactor;
        float polygonOffsetUnits;
        int shadeModel;
        float[] color;
        boolean[] colorMask;
        double clearDepth;
        float lightmapX;
        float lightmapY;
        Entity renderViewEntity;
        World renderManagerWorld;

        static GlStateSnapshot capture() {
            GlStateSnapshot state = new GlStateSnapshot();
            state.entryGlError = drainGlErrors();
            state.matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
            state.activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            state.clientActiveTexture = GL11.glGetInteger(GL13.GL_CLIENT_ACTIVE_TEXTURE);
            state.alphaEnabled = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
            state.alphaFunction = GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);
            state.alphaReference = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
            state.depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            state.depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            state.depthFunction = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
            state.blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
            state.blendSourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
            state.blendDestinationRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
            state.blendSourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
            state.blendDestinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
            state.lightingEnabled = GL11.glIsEnabled(GL11.GL_LIGHTING);
            for (int i = 0; i < state.lightsEnabled.length; i++)
                state.lightsEnabled[i] = GL11.glIsEnabled(GL11.GL_LIGHT0 + i);
            state.colorMaterialEnabled = GL11.glIsEnabled(GL11.GL_COLOR_MATERIAL);
            state.normalizeEnabled = GL11.glIsEnabled(GL11.GL_NORMALIZE);
            state.rescaleNormalEnabled = GL11.glIsEnabled(GL12.GL_RESCALE_NORMAL);
            state.fogEnabled = GL11.glIsEnabled(GL11.GL_FOG);
            state.cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            state.scissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
            state.polygonOffsetEnabled = GL11.glIsEnabled(GL11.GL_POLYGON_OFFSET_FILL);
            state.polygonOffsetFactor = GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_FACTOR);
            state.polygonOffsetUnits = GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_UNITS);
            state.shadeModel = GL11.glGetInteger(GL11.GL_SHADE_MODEL);
            state.color = floats(GL11.GL_CURRENT_COLOR, 4);
            state.colorMask = booleans(GL11.GL_COLOR_WRITEMASK, 4);
            state.clearDepth = GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);
            state.lightmapX = OpenGlHelper.lastBrightnessX;
            state.lightmapY = OpenGlHelper.lastBrightnessY;
            Minecraft minecraft = Minecraft.getMinecraft();
            state.renderViewEntity = minecraft.getRenderViewEntity();
            state.renderManagerWorld = minecraft.getRenderManager().world;

            try {
                state.modelViewStackDepth = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
                state.modelViewMatrix = matrix(GL11.GL_MODELVIEW, GL11.GL_MODELVIEW_MATRIX);
                state.projectionStackDepth = GL11.glGetInteger(GL11.GL_PROJECTION_STACK_DEPTH);
                state.projectionMatrix = matrix(GL11.GL_PROJECTION, GL11.GL_PROJECTION_MATRIX);
                for (int i = 0; i < TEXTURE_UNITS.length; i++) {
                    GL13.glActiveTexture(TEXTURE_UNITS[i]);
                    state.boundTextures[i] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
                    state.textureEnabled[i] = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
                    state.textureStackDepths[i] = GL11.glGetInteger(GL11.GL_TEXTURE_STACK_DEPTH);
                    state.textureMatrices[i] = matrix(GL11.GL_TEXTURE, GL11.GL_TEXTURE_MATRIX);
                }
            } finally {
                GL13.glActiveTexture(state.activeTexture);
                GL11.glMatrixMode(state.matrixMode);
            }
            state.queryGlError = drainGlErrors();
            return state;
        }

        String differencesFrom(GlStateSnapshot expected) {
            List<String> differences = new ArrayList<String>();
            different(differences, "matrixMode", expected.matrixMode, matrixMode);
            different(differences, "activeTexture", expected.activeTexture, activeTexture);
            different(differences, "clientActiveTexture", expected.clientActiveTexture, clientActiveTexture);
            different(differences, "modelViewStackDepth", expected.modelViewStackDepth, modelViewStackDepth);
            different(differences, "projectionStackDepth", expected.projectionStackDepth, projectionStackDepth);
            different(differences, "modelViewMatrix", expected.modelViewMatrix, modelViewMatrix);
            different(differences, "projectionMatrix", expected.projectionMatrix, projectionMatrix);
            different(differences, "alphaEnabled", expected.alphaEnabled, alphaEnabled);
            different(differences, "alphaFunction", expected.alphaFunction, alphaFunction);
            different(differences, "alphaReference", expected.alphaReference, alphaReference);
            different(differences, "depthEnabled", expected.depthEnabled, depthEnabled);
            different(differences, "depthMask", expected.depthMask, depthMask);
            different(differences, "depthFunction", expected.depthFunction, depthFunction);
            different(differences, "blendEnabled", expected.blendEnabled, blendEnabled);
            different(differences, "blendSourceRgb", expected.blendSourceRgb, blendSourceRgb);
            different(differences, "blendDestinationRgb", expected.blendDestinationRgb, blendDestinationRgb);
            different(differences, "blendSourceAlpha", expected.blendSourceAlpha, blendSourceAlpha);
            different(differences, "blendDestinationAlpha", expected.blendDestinationAlpha, blendDestinationAlpha);
            different(differences, "lightingEnabled", expected.lightingEnabled, lightingEnabled);
            different(differences, "lightsEnabled", expected.lightsEnabled, lightsEnabled);
            different(differences, "colorMaterialEnabled", expected.colorMaterialEnabled, colorMaterialEnabled);
            different(differences, "normalizeEnabled", expected.normalizeEnabled, normalizeEnabled);
            different(differences, "rescaleNormalEnabled", expected.rescaleNormalEnabled, rescaleNormalEnabled);
            different(differences, "fogEnabled", expected.fogEnabled, fogEnabled);
            different(differences, "cullEnabled", expected.cullEnabled, cullEnabled);
            different(differences, "scissorEnabled", expected.scissorEnabled, scissorEnabled);
            different(differences, "polygonOffsetEnabled", expected.polygonOffsetEnabled, polygonOffsetEnabled);
            different(differences, "polygonOffsetFactor", expected.polygonOffsetFactor, polygonOffsetFactor);
            different(differences, "polygonOffsetUnits", expected.polygonOffsetUnits, polygonOffsetUnits);
            different(differences, "shadeModel", expected.shadeModel, shadeModel);
            different(differences, "color", expected.color, color);
            different(differences, "colorMask", expected.colorMask, colorMask);
            different(differences, "clearDepth", expected.clearDepth, clearDepth);
            different(differences, "lightmapX", expected.lightmapX, lightmapX);
            different(differences, "lightmapY", expected.lightmapY, lightmapY);
            if (expected.renderViewEntity != renderViewEntity)
                differences.add("renderViewEntity=" + identity(expected.renderViewEntity) + "->" + identity(renderViewEntity));
            if (expected.renderManagerWorld != renderManagerWorld)
                differences.add("renderManagerWorld=" + identity(expected.renderManagerWorld) + "->" + identity(renderManagerWorld));
            for (int i = 0; i < TEXTURE_UNITS.length; i++) {
                String unit = "texture" + i;
                different(differences, unit + "StackDepth", expected.textureStackDepths[i], textureStackDepths[i]);
                different(differences, unit + "Matrix", expected.textureMatrices[i], textureMatrices[i]);
                different(differences, unit + "Binding", expected.boundTextures[i], boundTextures[i]);
                different(differences, unit + "Enabled", expected.textureEnabled[i], textureEnabled[i]);
            }
            return join(differences);
        }

        private static float[] matrix(int mode, int name) {
            GL11.glMatrixMode(mode);
            return floats(name, 16);
        }

        private static float[] floats(int name, int size) {
            FloatBuffer values = BufferUtils.createFloatBuffer(size);
            GL11.glGetFloat(name, values);
            float[] result = new float[size];
            for (int i = 0; i < size; i++) result[i] = values.get(i);
            return result;
        }

        private static boolean[] booleans(int name, int size) {
            java.nio.ByteBuffer values = BufferUtils.createByteBuffer(size);
            GL11.glGetBoolean(name, values);
            boolean[] result = new boolean[size];
            for (int i = 0; i < size; i++) result[i] = values.get(i) != 0;
            return result;
        }

        private static int drainGlErrors() {
            int first = GL11.GL_NO_ERROR;
            int error;
            while ((error = GL11.glGetError()) != GL11.GL_NO_ERROR)
                if (first == GL11.GL_NO_ERROR) first = error;
            return first;
        }

        private static void different(List<String> values, String name, int expected, int actual) {
            if (expected != actual) values.add(name + "=" + expected + "->" + actual);
        }

        private static void different(List<String> values, String name, boolean expected, boolean actual) {
            if (expected != actual) values.add(name + "=" + expected + "->" + actual);
        }

        private static void different(List<String> values, String name, float expected, float actual) {
            if (!same(expected, actual)) values.add(name + "=" + expected + "->" + actual);
        }

        private static void different(List<String> values, String name, double expected, double actual) {
            if (Math.abs(expected - actual) > 1.0e-9) values.add(name + "=" + expected + "->" + actual);
        }

        private static void different(List<String> values, String name, float[] expected, float[] actual) {
            if (expected == null || actual == null || expected.length != actual.length) {
                values.add(name + "=shape-changed");
                return;
            }
            for (int i = 0; i < expected.length; i++) {
                if (!same(expected[i], actual[i])) {
                    values.add(name + "[" + i + "]=" + expected[i] + "->" + actual[i]);
                    return;
                }
            }
        }

        private static void different(List<String> values, String name, boolean[] expected, boolean[] actual) {
            if (expected == null || actual == null || expected.length != actual.length) {
                values.add(name + "=shape-changed");
                return;
            }
            for (int i = 0; i < expected.length; i++) {
                if (expected[i] != actual[i]) {
                    values.add(name + "[" + i + "]=" + expected[i] + "->" + actual[i]);
                    return;
                }
            }
        }

        private static boolean same(float expected, float actual) {
            return Float.floatToIntBits(expected) == Float.floatToIntBits(actual)
                || Math.abs(expected - actual) <= 1.0e-6f;
        }

        private static String identity(Object value) {
            return value == null ? "null" : value.getClass().getSimpleName() + "@"
                + Integer.toHexString(System.identityHashCode(value));
        }

        private static String join(List<String> values) {
            StringBuilder result = new StringBuilder();
            for (String value : values) {
                if (result.length() > 0) result.append(", ");
                result.append(value);
            }
            return result.toString();
        }
    }

    private static String glError(int error) {
        return error == GL11.GL_NO_ERROR ? "GL_NO_ERROR" : "0x" + Integer.toHexString(error);
    }

    public static final class CameraState {
        public final float yaw;
        public final float pitch;
        public final float zoom;

        CameraState(float yaw, float pitch, float zoom) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.zoom = zoom;
        }

        public boolean differsFrom(CameraState other) {
            return other == null || Math.abs(yaw - other.yaw) > .001f
                || Math.abs(pitch - other.pitch) > .001f || Math.abs(zoom - other.zoom) > .001f;
        }

        @Override public String toString() {
            return "yaw=" + yaw + ", pitch=" + pitch + ", zoom=" + zoom;
        }
    }

    public static final class CacheState {
        public final int sections;
        public final int dirty;
        public final int clean;
        public final int cachedTiles;
        public final EnumSet<BlockRenderLayer> layers;
        private final EnumMap<BlockRenderLayer, Integer> vertices;

        CacheState(int sections, int dirty, int clean, int cachedTiles, EnumSet<BlockRenderLayer> layers,
                   EnumMap<BlockRenderLayer, Integer> vertices) {
            this.sections = sections;
            this.dirty = dirty;
            this.clean = clean;
            this.cachedTiles = cachedTiles;
            this.layers = layers.isEmpty() ? EnumSet.noneOf(BlockRenderLayer.class) : EnumSet.copyOf(layers);
            this.vertices = new EnumMap<BlockRenderLayer, Integer>(vertices);
        }

        public int vertexCount(BlockRenderLayer layer) {
            Integer count = vertices.get(layer);
            return count == null ? 0 : count.intValue();
        }

        public int totalVertices() {
            int total = 0;
            for (Integer count : vertices.values()) total += count.intValue();
            return total;
        }

        public List<String> layerNames() {
            List<String> names = new ArrayList<String>();
            for (BlockRenderLayer layer : layers) names.add(layer.name());
            Collections.sort(names);
            return names;
        }

        @Override public String toString() {
            return "sections=" + sections + ", dirty=" + dirty + ", clean=" + clean
                + ", cachedTiles=" + cachedTiles + ", vertices=" + vertices;
        }
    }

    public static final class SectionSnapshot {
        public final int sections;
        public final int temporarySections;
        public final int visibleSections;
        public final int partialSections;
        public final int floorBlocks;
        public final int upperBlocks;
        public final int visibleBlocks;
        private final List<BlockPos> visiblePositions;

        SectionSnapshot(int sections, int temporarySections, int visibleSections, int partialSections,
                        int floorBlocks, int upperBlocks, Set<BlockPos> visiblePositions) {
            this.sections = sections;
            this.temporarySections = temporarySections;
            this.visibleSections = visibleSections;
            this.partialSections = partialSections;
            this.floorBlocks = floorBlocks;
            this.upperBlocks = upperBlocks;
            this.visibleBlocks = visiblePositions.size();
            this.visiblePositions = Collections.unmodifiableList(new ArrayList<BlockPos>(visiblePositions));
        }

        public boolean containsVisible(BlockPos pos) {
            return pos != null && visiblePositions.contains(pos);
        }

        @Override public String toString() {
            return "sections=" + sections + ", temporary=" + temporarySections + ", visibleSections="
                + visibleSections + ", partial=" + partialSections + ", floor=" + floorBlocks
                + ", upper=" + upperBlocks + ", visibleBlocks=" + visibleBlocks;
        }
    }

    public static final class DepthSnapshot {
        public final long frame;
        public final int framebufferWidth;
        public final int framebufferHeight;
        public final int samples;
        public final int backgroundSamples;
        public final int sceneSamples;
        public final int unexpectedSamples;
        public final float minimum;
        public final float maximum;

        DepthSnapshot(long frame, int framebufferWidth, int framebufferHeight, int samples,
                      int backgroundSamples, int sceneSamples, int unexpectedSamples,
                      float minimum, float maximum) {
            this.frame = frame;
            this.framebufferWidth = framebufferWidth;
            this.framebufferHeight = framebufferHeight;
            this.samples = samples;
            this.backgroundSamples = backgroundSamples;
            this.sceneSamples = sceneSamples;
            this.unexpectedSamples = unexpectedSamples;
            this.minimum = minimum;
            this.maximum = maximum;
        }

        public boolean hasBackground() {
            return backgroundSamples > 0;
        }

        public boolean hasSceneGeometry() {
            return sceneSamples > 0;
        }

        @Override public String toString() {
            return "frame=" + frame + ", framebuffer=" + framebufferWidth + "x" + framebufferHeight
                + ", samples=" + samples + ", background=" + backgroundSamples + ", scene=" + sceneSamples
                + ", unexpected=" + unexpectedSamples + ", range=" + minimum + ".." + maximum;
        }
    }

    private static final class DepthAccumulator {
        private int samples;
        private int backgroundSamples;
        private int sceneSamples;
        private int unexpectedSamples;
        private float minimum = 1;
        private float maximum;

        void accept(float depth) {
            samples++;
            if (Float.isNaN(depth) || Float.isInfinite(depth) || depth < 0 || depth > 1) {
                unexpectedSamples++;
                return;
            }
            minimum = Math.min(minimum, depth);
            maximum = Math.max(maximum, depth);
            if (depth >= .999999f) backgroundSamples++;
            else if (depth >= .9f) sceneSamples++;
            else unexpectedSamples++;
        }

        DepthSnapshot snapshot(long frame, int framebufferWidth, int framebufferHeight) {
            return new DepthSnapshot(frame, framebufferWidth, framebufferHeight, samples,
                backgroundSamples, sceneSamples, unexpectedSamples, minimum, maximum);
        }
    }
}
