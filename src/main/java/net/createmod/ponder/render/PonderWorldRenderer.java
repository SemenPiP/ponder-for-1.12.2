package net.createmod.ponder.render;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

import com.mojang.authlib.GameProfile;

import net.createmod.catnip.render.GlStateGuard;
import net.createmod.ponder.api.element.PonderOverlayElement;
import net.createmod.ponder.api.element.PonderSceneElement;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.PonderWorld;
import net.createmod.ponder.foundation.PonderWorld.BreakProgress;
import net.createmod.ponder.foundation.PonderWorld.ParticleEvent;
import net.createmod.ponder.foundation.element.OverlayDataElement;
import net.createmod.ponder.foundation.element.WorldSectionElementImpl;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Fixed-pipeline renderer for the isolated scene world. */
@SideOnly(Side.CLIENT)
public final class PonderWorldRenderer {
    private static final Logger LOGGER = LogManager.getLogger("PonderWorldRenderer");

    private final Map<PonderWorld, ParticleManager> particleManagers =
        new WeakHashMap<PonderWorld, ParticleManager>();
    private final Map<PonderWorld, EntityOtherPlayerMP> virtualCameras =
        new WeakHashMap<PonderWorld, EntityOtherPlayerMP>();
    private final Map<PonderWorld, Long> particleVersions =
        new WeakHashMap<PonderWorld, Long>();

    public void tick(PonderWorld world) {
        ParticleManager manager = particleManager(world);
        for (ParticleEvent particle : world.drainParticles()) {
            try {
                manager.spawnEffectParticle(particle.type.getParticleID(), particle.x, particle.y, particle.z,
                    particle.velocityX, particle.velocityY, particle.velocityZ, particle.parameters);
            } catch (Throwable throwable) {
                LOGGER.warn("Could not create virtual particle {}", particle.type, throwable);
            }
        }
        try {
            manager.updateEffects();
        } catch (Throwable throwable) {
            LOGGER.warn("Could not tick virtual scene particles", throwable);
        }
    }

    public void render(PonderWorld world, Iterable<PonderSceneElement> elements,
                       Vec3d camera, float partialTicks) {
        RenderManager manager = Minecraft.getMinecraft().getRenderManager();
        render(world, elements, camera, camera, manager.playerViewY, manager.playerViewX,
            partialTicks, true);
    }

    public void renderScene(PonderScene scene, Vec3d camera, float partialTicks) {
        RenderManager manager = Minecraft.getMinecraft().getRenderManager();
        renderScene(scene, camera, camera, manager.playerViewY, manager.playerViewX, partialTicks);
    }

    /**
     * Draw and sorting cameras are separate because the Ponder UI installs its view transform
     * in the model-view matrix. Draw coordinates therefore remain scene-local while transparent
     * sorting and third-party renderer state still need the real scene-space camera.
     */
    public void renderScene(PonderScene scene, Vec3d drawCamera, Vec3d sortingCamera,
                            float viewYaw, float viewPitch, float partialTicks) {
        render(scene.getWorld(), scene.getSceneElements(), drawCamera, sortingCamera,
            viewYaw, viewPitch, partialTicks, false);
        for (PonderOverlayElement overlay : scene.getOverlayElements()) {
            if (overlay instanceof OverlayDataElement) {
                PonderOverlayRenderer.render((OverlayDataElement) overlay, drawCamera, partialTicks);
            }
        }
    }

    private void render(PonderWorld world, Iterable<PonderSceneElement> sourceElements,
                        Vec3d drawCamera, Vec3d sortingCamera, float viewYaw, float viewPitch,
                        float partialTicks, boolean renderRawWorld) {
        List<PonderSceneElement> elements = new ArrayList<PonderSceneElement>();
        for (PonderSceneElement element : sourceElements) {
            elements.add(element);
        }
        EntityOtherPlayerMP cameraEntity = virtualCamera(world, sortingCamera, viewYaw, viewPitch);
        final float previousLightmapX = OpenGlHelper.lastBrightnessX;
        final float previousLightmapY = OpenGlHelper.lastBrightnessY;
        try (GlStateGuard ignored = GlStateGuard.capture();
             VirtualRenderState virtualState = VirtualRenderState.install(world, cameraEntity,
                 sortingCamera, viewYaw, viewPitch)) {
            configureFixedPipeline();
            for (PonderSceneElement element : elements) {
                if (element.isVisible()) {
                    safeFirst(element, world, drawCamera, sortingCamera, partialTicks);
                }
            }
            ITextureObject blockAtlas = Minecraft.getMinecraft().getTextureManager()
                .getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            for (BlockRenderLayer layer : BlockRenderLayer.values()) {
                configureLayer(layer);
                boolean restoreFiltering = configureLayerFiltering(blockAtlas, layer);
                try {
                    if (renderRawWorld) {
                        renderLayer(world, world.getOccupiedPositions(), layer, drawCamera);
                    }
                    renderElementLayer(elements, world, layer, drawCamera, sortingCamera, partialTicks);
                } finally {
                    if (restoreFiltering) blockAtlas.restoreLastBlurMipmap();
                }
            }
            configurePostLayerPass();
            if (renderRawWorld) {
                renderTileEntities(world, world.getTileEntities(), drawCamera, sortingCamera, partialTicks);
                renderEntities(world.getEntities(), drawCamera, partialTicks);
                renderBreaking(world, drawCamera);
            }
            renderParticles(world, drawCamera, viewYaw, viewPitch, partialTicks);
            configurePostLayerPass();
            for (PonderSceneElement element : elements) {
                if (element.isVisible()) {
                    safeLast(element, world, drawCamera, sortingCamera, partialTicks);
                }
            }
        } finally {
            try {
                ForgeHooksClient.setRenderLayer(null);
            } finally {
                // GlStateGuard restores the actual GL texture state; only repair Minecraft's
                // bookkeeping without changing the active texture unit after the guard closes.
                OpenGlHelper.lastBrightnessX = previousLightmapX;
                OpenGlHelper.lastBrightnessY = previousLightmapY;
            }
        }
    }

    private void renderElementLayer(List<PonderSceneElement> elements, PonderWorld world,
                                    BlockRenderLayer layer, Vec3d drawCamera, final Vec3d sortingCamera,
                                    final float partialTicks) {
        if (layer != BlockRenderLayer.TRANSLUCENT) {
            for (PonderSceneElement element : elements) {
                if (element.isVisible()) {
                    safeLayer(element, world, layer, drawCamera, sortingCamera, partialTicks);
                }
            }
            return;
        }

        List<WorldSectionElementImpl> sections = new ArrayList<WorldSectionElementImpl>();
        for (PonderSceneElement element : elements) {
            if (!element.isVisible()) {
                continue;
            }
            if (element instanceof WorldSectionElementImpl) {
                sections.add((WorldSectionElementImpl) element);
            } else {
                safeLayer(element, world, layer, drawCamera, sortingCamera, partialTicks);
            }
        }
        Collections.sort(sections, new Comparator<WorldSectionElementImpl>() {
            @Override
            public int compare(WorldSectionElementImpl first, WorldSectionElementImpl second) {
                return Double.compare(second.getSortDistance(sortingCamera, partialTicks),
                    first.getSortDistance(sortingCamera, partialTicks));
            }
        });
        for (WorldSectionElementImpl section : sections) {
            safeLayer(section, world, layer, drawCamera, sortingCamera, partialTicks);
        }
    }

    public void renderLayer(PonderWorld world, Iterable<BlockPos> positions,
                            BlockRenderLayer layer, Vec3d camera) {
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        BlockRendererDispatcher dispatcher = minecraft.getBlockRendererDispatcher();
        BufferBuilder aggregate = new BufferBuilder(262144);
        aggregate.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        ForgeHooksClient.setRenderLayer(layer);
        try {
            for (BlockPos pos : positions) {
                IBlockState state = world.getBlockState(pos);
                if (!state.getBlock().canRenderInLayer(state, layer)) {
                    continue;
                }
                BufferBuilder blockBuilder = new BufferBuilder(65536);
                blockBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
                blockBuilder.setTranslation(-camera.x, -camera.y, -camera.z);
                try (GlStateGuard ignored = GlStateGuard.capture()) {
                    dispatcher.renderBlock(state, pos, world, blockBuilder);
                    if (blockBuilder.getVertexCount() > 0) {
                        aggregate.addVertexData(blockBuilder.getVertexState().getRawBuffer());
                    }
                } catch (Throwable throwable) {
                    LOGGER.warn("Block renderer failed for {} at {} in {}", state, pos, layer, throwable);
                }
            }
            if (aggregate.getVertexCount() > 0) {
                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder target = tessellator.getBuffer();
                boolean finished = false;
                try {
                    target.begin(GL11.GL_QUADS, aggregate.getVertexFormat());
                    target.setVertexState(aggregate.getVertexState());
                    if (layer == BlockRenderLayer.TRANSLUCENT) {
                        target.sortVertexData(0, 0, 0);
                    }
                    tessellator.draw();
                    finished = true;
                } finally {
                    if (!finished) {
                        finishSharedBuffer(tessellator);
                    }
                }
            }
        } finally {
            ForgeHooksClient.setRenderLayer(null);
        }
    }

    public void renderTileEntities(PonderWorld world, Collection<TileEntity> tiles,
                                   Vec3d drawCamera, float partialTicks) {
        renderTileEntities(world, tiles, drawCamera, drawCamera, partialTicks);
    }

    public void renderTileEntities(PonderWorld world, Collection<TileEntity> tiles,
                                   Vec3d drawCamera, Vec3d stateCamera, float partialTicks) {
        TileEntityRendererDispatcher dispatcher = TileEntityRendererDispatcher.instance;
        TileDispatcherState previous = TileDispatcherState.capture(dispatcher);
        Entity cameraEntity = Minecraft.getMinecraft().getRenderViewEntity();
        try {
            previous.installVirtual(dispatcher, world, cameraEntity, stateCamera);
            for (TileEntity tile : tiles) {
                if (tile == null || tile.isInvalid()) {
                    continue;
                }
                try (GlStateGuard ignored = GlStateGuard.capture()) {
                    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
                    dispatcher.render(tile, tile.getPos().getX() - drawCamera.x,
                        tile.getPos().getY() - drawCamera.y, tile.getPos().getZ() - drawCamera.z, partialTicks);
                } catch (Throwable throwable) {
                    finishSharedBuffer(Tessellator.getInstance());
                    LOGGER.warn("Tile renderer failed for {} at {}", tile.getClass().getName(),
                        tile.getPos(), throwable);
                }
            }
        } finally {
            previous.restore(dispatcher);
        }
    }

    public void renderEntities(Collection<Entity> entities, Vec3d camera, float partialTicks) {
        RenderManager manager = Minecraft.getMinecraft().getRenderManager();
        World previousWorld = manager.world;
        try {
            for (Entity entity : entities) {
                if (entity == null || entity.isDead) {
                    continue;
                }
                manager.setWorld(entity.world);
                double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - camera.x;
                double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - camera.y;
                double shadowY = y;
                double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - camera.z;
                float yaw = entity.prevRotationYaw
                    + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks;
                try (GlStateGuard ignored = GlStateGuard.capture()) {
                    Render<?> renderer = null;
                    boolean compensateBob = false;
                    if (entity.world instanceof PonderWorld && entity instanceof EntityItem) {
                        renderer = manager.getEntityRenderObject(entity);
                        compensateBob = renderer instanceof RenderEntityItem
                            && ((RenderEntityItem) renderer).shouldBob();
                        EntityItem item = (EntityItem) entity;
                        y = compensateItemBob(y, item.getAge(), partialTicks, item.hoverStart, compensateBob);
                    }
                    if (compensateBob && renderer != null) {
                        renderItemWithStableShadow(renderer, entity, x, y, shadowY, z, yaw, partialTicks);
                    } else {
                        manager.renderEntity(entity, x, y, z, yaw, partialTicks, false);
                    }
                } catch (Throwable throwable) {
                    finishSharedBuffer(Tessellator.getInstance());
                    LOGGER.warn("Entity renderer failed for {}", entity.getClass().getName(), throwable);
                }
            }
        } finally {
            manager.setWorld(previousWorld);
        }
    }

    @SuppressWarnings("unchecked")
    private static void renderItemWithStableShadow(Render<?> renderer, Entity entity, double x,
                                                   double modelY, double shadowY, double z,
                                                   float yaw, float partialTicks) {
        Render<Entity> typedRenderer = (Render<Entity>) renderer;
        typedRenderer.setRenderOutlines(false);
        typedRenderer.doRender(entity, x, modelY, z, yaw, partialTicks);
        typedRenderer.doRenderShadowAndFire(entity, x, shadowY, z, yaw, partialTicks);
    }

    static double compensateItemBob(double interpolatedY, int age, float partialTicks,
                                    float hoverStart, boolean shouldBob) {
        if (!shouldBob) {
            return interpolatedY;
        }
        float phase = ((float) age + partialTicks) / 10.0F + hoverStart;
        return interpolatedY - MathHelper.sin(phase) * 0.1F;
    }

    public void renderBreaking(PonderWorld world, Vec3d camera) {
        renderBreaking(world, null, camera);
    }

    public void renderBreaking(PonderWorld world, Selection selection, Vec3d camera) {
        if (world.getBlockBreakingProgress().isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        buffer.setTranslation(-camera.x, -camera.y, -camera.z);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.DST_COLOR,
            GlStateManager.DestFactor.SRC_COLOR);
        GlStateManager.enablePolygonOffset();
        GlStateManager.doPolygonOffset(-3, -3);
        boolean finished = false;
        try {
            for (BreakProgress progress : world.getBlockBreakingProgress().values()) {
                if (selection != null && !selection.test(progress.pos)) {
                    continue;
                }
                TextureAtlasSprite sprite = minecraft.getTextureMapBlocks()
                    .getAtlasSprite("minecraft:blocks/destroy_stage_" + progress.progress);
                try (GlStateGuard ignored = GlStateGuard.capture()) {
                    minecraft.getBlockRendererDispatcher().renderBlockDamage(
                        world.getBlockState(progress.pos), progress.pos, sprite, world);
                } catch (Throwable throwable) {
                    LOGGER.warn("Damage renderer failed at {}", progress.pos, throwable);
                }
            }
            tessellator.draw();
            finished = true;
        } finally {
            if (!finished) {
                finishSharedBuffer(tessellator);
            }
            buffer.setTranslation(0, 0, 0);
            GlStateManager.doPolygonOffset(0, 0);
            GlStateManager.disablePolygonOffset();
        }
    }

    private void renderParticles(PonderWorld world, Vec3d drawCamera,
                                 float viewYaw, float viewPitch, float partialTicks) {
        ParticleManager manager = particleManager(world);
        EntityOtherPlayerMP cameraEntity = virtualCamera(world, drawCamera, viewYaw, viewPitch);
        ParticleState previousParticles = ParticleState.capture();
        ActiveRenderState previousActive = ActiveRenderState.capture();
        try (GlStateGuard ignored = GlStateGuard.capture()) {
            previousActive.install(viewYaw, viewPitch);
            manager.renderParticles(cameraEntity, partialTicks);
            manager.renderLitParticles(cameraEntity, partialTicks);
        } catch (Throwable throwable) {
            finishSharedBuffer(Tessellator.getInstance());
            LOGGER.warn("Virtual particle renderer failed", throwable);
        } finally {
            previousParticles.restore();
            previousActive.restore();
        }
    }

    private ParticleManager particleManager(PonderWorld world) {
        ParticleManager manager = particleManagers.get(world);
        if (manager == null) {
            manager = new ParticleManager(world, Minecraft.getMinecraft().getTextureManager());
            particleManagers.put(world, manager);
        }
        Long version = particleVersions.get(world);
        if (version == null || version.longValue() != world.getStateVersion()) {
            manager.clearEffects(world);
            particleVersions.put(world, Long.valueOf(world.getStateVersion()));
        }
        return manager;
    }

    private EntityOtherPlayerMP virtualCamera(PonderWorld world, Vec3d position,
                                               float viewYaw, float viewPitch) {
        EntityOtherPlayerMP camera = virtualCameras.get(world);
        if (camera == null) {
            camera = new EntityOtherPlayerMP(world, new GameProfile(
                java.util.UUID.nameUUIDFromBytes("ponder-camera".getBytes()), "PonderCamera"));
            virtualCameras.put(world, camera);
        }
        camera.setWorld(world);
        camera.setPosition(position.x, position.y, position.z);
        camera.lastTickPosX = position.x;
        camera.lastTickPosY = position.y;
        camera.lastTickPosZ = position.z;
        camera.prevRotationYaw = camera.rotationYaw = viewYaw;
        camera.prevRotationPitch = camera.rotationPitch = viewPitch;
        return camera;
    }

    private static void configureFixedPipeline() {
        Minecraft minecraft = Minecraft.getMinecraft();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.enableDepth();
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, .1f);
        GlStateManager.disableFog();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.color(1, 1, 1, 1);

        minecraft.entityRenderer.enableLightmap();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.enableTexture2D();
        minecraft.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
    }

    private static void configurePostLayerPass() {
        GlStateManager.enableDepth();
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.depthMask(true);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, .1f);
        GlStateManager.disableBlend();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.enableTexture2D();
        GlStateManager.color(1, 1, 1, 1);
    }

    private static void configureLayer(BlockRenderLayer layer) {
        Minecraft minecraft = Minecraft.getMinecraft();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.enableTexture2D();
        minecraft.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableDepth();
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.depthMask(layer != BlockRenderLayer.TRANSLUCENT);
        if (layer == BlockRenderLayer.TRANSLUCENT) {
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        } else {
            GlStateManager.disableBlend();
        }
        if (layer == BlockRenderLayer.SOLID) {
            GlStateManager.disableAlpha();
        } else {
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(GL11.GL_GREATER, .1f);
        }
    }

    private static boolean configureLayerFiltering(ITextureObject blockAtlas, BlockRenderLayer layer) {
        if (layer == BlockRenderLayer.CUTOUT_MIPPED) {
            blockAtlas.setBlurMipmap(false, Minecraft.getMinecraft().gameSettings.mipmapLevels > 0);
            return true;
        }
        if (layer == BlockRenderLayer.CUTOUT) {
            blockAtlas.setBlurMipmap(false, false);
            return true;
        }
        return false;
    }

    private static void safeFirst(PonderSceneElement element, PonderWorld world,
                                  Vec3d drawCamera, Vec3d sortingCamera, float partialTicks) {
        try (GlStateGuard ignored = GlStateGuard.capture()) {
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.pushMatrix();
            try {
                GlStateManager.translate(-drawCamera.x, -drawCamera.y, -drawCamera.z);
                if (element instanceof WorldSectionElementImpl) {
                    ((WorldSectionElementImpl) element).renderFirst(world, partialTicks, sortingCamera);
                } else {
                    element.renderFirst(world, partialTicks);
                }
            } finally {
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                GlStateManager.popMatrix();
            }
        } catch (Throwable throwable) {
            finishSharedBuffer(Tessellator.getInstance());
            LOGGER.warn("Scene element first pass failed: {}", element, throwable);
        }
    }

    private static void safeLayer(PonderSceneElement element, PonderWorld world, BlockRenderLayer layer,
                                  Vec3d drawCamera, Vec3d sortingCamera, float partialTicks) {
        try (GlStateGuard ignored = GlStateGuard.capture()) {
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.pushMatrix();
            try {
                GlStateManager.translate(-drawCamera.x, -drawCamera.y, -drawCamera.z);
                if (element instanceof WorldSectionElementImpl) {
                    ((WorldSectionElementImpl) element).renderLayer(world, layer, partialTicks, sortingCamera);
                } else {
                    element.renderLayer(world, layer, partialTicks);
                }
            } finally {
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                GlStateManager.popMatrix();
            }
        } catch (Throwable throwable) {
            finishSharedBuffer(Tessellator.getInstance());
            LOGGER.warn("Scene element layer failed: {}", element, throwable);
        }
    }

    private static void safeLast(PonderSceneElement element, PonderWorld world,
                                 Vec3d drawCamera, Vec3d sortingCamera, float partialTicks) {
        try (GlStateGuard ignored = GlStateGuard.capture()) {
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.pushMatrix();
            try {
                GlStateManager.translate(-drawCamera.x, -drawCamera.y, -drawCamera.z);
                if (element instanceof WorldSectionElementImpl) {
                    ((WorldSectionElementImpl) element).renderLast(world, partialTicks, sortingCamera);
                } else {
                    element.renderLast(world, partialTicks);
                }
            } finally {
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                GlStateManager.popMatrix();
            }
        } catch (Throwable throwable) {
            finishSharedBuffer(Tessellator.getInstance());
            LOGGER.warn("Scene element last pass failed: {}", element, throwable);
        }
    }

    private static final class VirtualRenderState implements AutoCloseable {
        private final Minecraft minecraft;
        private final Entity previousViewEntity;
        private final RenderManagerState renderManager;
        private final TileDispatcherState tileDispatcher;
        private final ActiveRenderState activeRender;

        private VirtualRenderState(PonderWorld world, EntityOtherPlayerMP camera, Vec3d cameraPosition,
                                   float viewYaw, float viewPitch) {
            minecraft = Minecraft.getMinecraft();
            previousViewEntity = minecraft.getRenderViewEntity();
            renderManager = RenderManagerState.capture(minecraft.getRenderManager());
            tileDispatcher = TileDispatcherState.capture(TileEntityRendererDispatcher.instance);
            activeRender = ActiveRenderState.capture();
            boolean installed = false;
            try {
                minecraft.setRenderViewEntity(camera);
                renderManager.installVirtual(minecraft.getRenderManager(), world, camera, cameraPosition,
                    viewYaw, viewPitch);
                tileDispatcher.installVirtual(TileEntityRendererDispatcher.instance, world, camera,
                    cameraPosition);
                activeRender.install(viewYaw, viewPitch);
                installed = true;
            } finally {
                if (!installed) {
                    close();
                }
            }
        }

        private static VirtualRenderState install(PonderWorld world, EntityOtherPlayerMP camera,
                                                  Vec3d cameraPosition, float viewYaw, float viewPitch) {
            return new VirtualRenderState(world, camera, cameraPosition, viewYaw, viewPitch);
        }

        @Override
        public void close() {
            try {
                activeRender.restore();
            } finally {
                try {
                    tileDispatcher.restore(TileEntityRendererDispatcher.instance);
                } finally {
                    try {
                        renderManager.restore(minecraft.getRenderManager());
                    } finally {
                        minecraft.setRenderViewEntity(previousViewEntity);
                    }
                }
            }
        }
    }

    private static final class RenderManagerState {
        private static final Field RENDER_POS_X = findField(RenderManager.class,
            "renderPosX", "field_78725_b");
        private static final Field RENDER_POS_Y = findField(RenderManager.class,
            "renderPosY", "field_78726_c");
        private static final Field RENDER_POS_Z = findField(RenderManager.class,
            "renderPosZ", "field_78723_d");

        private final World world;
        private final Entity renderViewEntity;
        private final Entity pointedEntity;
        private final float playerViewY;
        private final float playerViewX;
        private final double viewerPosX;
        private final double viewerPosY;
        private final double viewerPosZ;
        private final double renderPosX;
        private final double renderPosY;
        private final double renderPosZ;

        private RenderManagerState(RenderManager manager) {
            world = manager.world;
            renderViewEntity = manager.renderViewEntity;
            pointedEntity = manager.pointedEntity;
            playerViewY = manager.playerViewY;
            playerViewX = manager.playerViewX;
            viewerPosX = manager.viewerPosX;
            viewerPosY = manager.viewerPosY;
            viewerPosZ = manager.viewerPosZ;
            renderPosX = getDouble(RENDER_POS_X, manager);
            renderPosY = getDouble(RENDER_POS_Y, manager);
            renderPosZ = getDouble(RENDER_POS_Z, manager);
        }

        private static RenderManagerState capture(RenderManager manager) {
            return new RenderManagerState(manager);
        }

        private void installVirtual(RenderManager manager, World virtualWorld, Entity camera,
                                    Vec3d position, float yaw, float pitch) {
            manager.setWorld(virtualWorld);
            manager.renderViewEntity = camera;
            manager.pointedEntity = null;
            manager.playerViewY = yaw;
            manager.playerViewX = pitch;
            manager.viewerPosX = position.x;
            manager.viewerPosY = position.y;
            manager.viewerPosZ = position.z;
            manager.setRenderPosition(position.x, position.y, position.z);
        }

        private void restore(RenderManager manager) {
            manager.setWorld(world);
            manager.renderViewEntity = renderViewEntity;
            manager.pointedEntity = pointedEntity;
            manager.playerViewY = playerViewY;
            manager.playerViewX = playerViewX;
            manager.viewerPosX = viewerPosX;
            manager.viewerPosY = viewerPosY;
            manager.viewerPosZ = viewerPosZ;
            manager.setRenderPosition(renderPosX, renderPosY, renderPosZ);
        }
    }

    private static final class TileDispatcherState {
        private final World world;
        private final Entity entity;
        private final float entityYaw;
        private final float entityPitch;
        private final RayTraceResult cameraHitResult;
        private final double entityX;
        private final double entityY;
        private final double entityZ;
        private final double staticPlayerX;
        private final double staticPlayerY;
        private final double staticPlayerZ;

        private TileDispatcherState(TileEntityRendererDispatcher dispatcher) {
            world = dispatcher.world;
            entity = dispatcher.entity;
            entityYaw = dispatcher.entityYaw;
            entityPitch = dispatcher.entityPitch;
            cameraHitResult = dispatcher.cameraHitResult;
            entityX = dispatcher.entityX;
            entityY = dispatcher.entityY;
            entityZ = dispatcher.entityZ;
            staticPlayerX = TileEntityRendererDispatcher.staticPlayerX;
            staticPlayerY = TileEntityRendererDispatcher.staticPlayerY;
            staticPlayerZ = TileEntityRendererDispatcher.staticPlayerZ;
        }

        private static TileDispatcherState capture(TileEntityRendererDispatcher dispatcher) {
            return new TileDispatcherState(dispatcher);
        }

        private void installVirtual(TileEntityRendererDispatcher dispatcher, World virtualWorld,
                                    Entity camera, Vec3d position) {
            dispatcher.setWorld(virtualWorld);
            dispatcher.entity = camera;
            dispatcher.entityYaw = camera == null ? 0 : camera.rotationYaw;
            dispatcher.entityPitch = camera == null ? 0 : camera.rotationPitch;
            dispatcher.cameraHitResult = null;
            dispatcher.entityX = position.x;
            dispatcher.entityY = position.y;
            dispatcher.entityZ = position.z;
            TileEntityRendererDispatcher.staticPlayerX = position.x;
            TileEntityRendererDispatcher.staticPlayerY = position.y;
            TileEntityRendererDispatcher.staticPlayerZ = position.z;
        }

        private void restore(TileEntityRendererDispatcher dispatcher) {
            dispatcher.setWorld(world);
            dispatcher.entity = entity;
            dispatcher.entityYaw = entityYaw;
            dispatcher.entityPitch = entityPitch;
            dispatcher.cameraHitResult = cameraHitResult;
            dispatcher.entityX = entityX;
            dispatcher.entityY = entityY;
            dispatcher.entityZ = entityZ;
            TileEntityRendererDispatcher.staticPlayerX = staticPlayerX;
            TileEntityRendererDispatcher.staticPlayerY = staticPlayerY;
            TileEntityRendererDispatcher.staticPlayerZ = staticPlayerZ;
        }
    }

    private static final class ActiveRenderState {
        private static final Field POSITION = findField(ActiveRenderInfo.class,
            "position", "field_178811_e");
        private static final Field ROTATION_X = findField(ActiveRenderInfo.class,
            "rotationX", "field_74588_d");
        private static final Field ROTATION_XZ = findField(ActiveRenderInfo.class,
            "rotationXZ", "field_74589_e");
        private static final Field ROTATION_Z = findField(ActiveRenderInfo.class,
            "rotationZ", "field_74586_f");
        private static final Field ROTATION_YZ = findField(ActiveRenderInfo.class,
            "rotationYZ", "field_74587_g");
        private static final Field ROTATION_XY = findField(ActiveRenderInfo.class,
            "rotationXY", "field_74596_h");

        private final Vec3d position;
        private final float rotationX;
        private final float rotationXZ;
        private final float rotationZ;
        private final float rotationYZ;
        private final float rotationXY;

        private ActiveRenderState() {
            position = (Vec3d) get(POSITION, null);
            rotationX = getFloat(ROTATION_X, null);
            rotationXZ = getFloat(ROTATION_XZ, null);
            rotationZ = getFloat(ROTATION_Z, null);
            rotationYZ = getFloat(ROTATION_YZ, null);
            rotationXY = getFloat(ROTATION_XY, null);
        }

        private static ActiveRenderState capture() {
            return new ActiveRenderState();
        }

        private void install(float yaw, float pitch) {
            float yawRadians = (float) Math.toRadians(yaw);
            float pitchRadians = (float) Math.toRadians(pitch);
            float x = (float) Math.cos(yawRadians);
            float z = (float) Math.sin(yawRadians);
            set(POSITION, null, Vec3d.ZERO);
            setFloat(ROTATION_X, null, x);
            setFloat(ROTATION_Z, null, z);
            setFloat(ROTATION_YZ, null, -z * (float) Math.sin(pitchRadians));
            setFloat(ROTATION_XY, null, x * (float) Math.sin(pitchRadians));
            setFloat(ROTATION_XZ, null, (float) Math.cos(pitchRadians));
        }

        private void restore() {
            set(POSITION, null, position);
            setFloat(ROTATION_X, null, rotationX);
            setFloat(ROTATION_XZ, null, rotationXZ);
            setFloat(ROTATION_Z, null, rotationZ);
            setFloat(ROTATION_YZ, null, rotationYZ);
            setFloat(ROTATION_XY, null, rotationXY);
        }
    }

    private static final class ParticleState {
        private final double interpPosX = Particle.interpPosX;
        private final double interpPosY = Particle.interpPosY;
        private final double interpPosZ = Particle.interpPosZ;
        private final Vec3d cameraViewDir = Particle.cameraViewDir;

        private static ParticleState capture() {
            return new ParticleState();
        }

        private void restore() {
            Particle.interpPosX = interpPosX;
            Particle.interpPosY = interpPosY;
            Particle.interpPosZ = interpPosZ;
            Particle.cameraViewDir = cameraViewDir;
        }
    }

    private static Field findField(Class<?> owner, String deobfuscated, String obfuscated) {
        Field field = ReflectionHelper.findField(owner, deobfuscated, obfuscated);
        field.setAccessible(true);
        return field;
    }

    private static Object get(Field field, Object owner) {
        try {
            return field.get(owner);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Could not capture render state", exception);
        }
    }

    private static double getDouble(Field field, Object owner) {
        try {
            return field.getDouble(owner);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Could not capture render state", exception);
        }
    }

    private static float getFloat(Field field, Object owner) {
        try {
            return field.getFloat(owner);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Could not capture render state", exception);
        }
    }

    private static void set(Field field, Object owner, Object value) {
        try {
            field.set(owner, value);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Could not restore render state", exception);
        }
    }

    private static void setFloat(Field field, Object owner, float value) {
        try {
            field.setFloat(owner, value);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Could not restore render state", exception);
        }
    }

    private static void finishSharedBuffer(Tessellator tessellator) {
        try {
            tessellator.draw();
        } catch (Throwable ignored) {
            // Either the failed renderer left a batch open and this call closed it, or the
            // original draw already closed it and this is the expected "Not building" path.
        }
    }
}
