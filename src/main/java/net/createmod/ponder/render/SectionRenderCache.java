package net.createmod.ponder.render;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

import net.createmod.catnip.render.GlStateGuard;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderWorld;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.client.ForgeHooksClient;

/** One draw-state per vanilla block layer for an independently transformable section. */
public final class SectionRenderCache {
    private static final Logger LOGGER = LogManager.getLogger("PonderSectionRenderCache");
    private static final Map<SectionRenderCache, Boolean> LIVE_CACHES =
        Collections.synchronizedMap(new WeakHashMap<SectionRenderCache, Boolean>());

    private final Map<BlockRenderLayer, BufferBuilder.State> layers =
        new EnumMap<BlockRenderLayer, BufferBuilder.State>(BlockRenderLayer.class);
    private final List<TileEntity> tiles = new ArrayList<TileEntity>();
    private boolean dirty = true;

    public SectionRenderCache() {
        LIVE_CACHES.put(this, Boolean.TRUE);
    }

    /** Called after a texture/model resource reload. */
    public static void invalidateAll() {
        synchronized (LIVE_CACHES) {
            for (SectionRenderCache cache : new ArrayList<SectionRenderCache>(LIVE_CACHES.keySet())) {
                if (cache != null) {
                    cache.invalidate();
                }
            }
        }
    }

    public void invalidate() {
        dirty = true;
    }

    public void ensureBuilt(PonderWorld world, Selection selection) {
        if (!dirty) {
            return;
        }
        layers.clear();
        tiles.clear();
        IBlockAccess maskedWorld = createSelectionView(world, selection);
        for (BlockRenderLayer layer : BlockRenderLayer.values()) {
            BufferBuilder aggregate = new BufferBuilder(262144);
            aggregate.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
            ForgeHooksClient.setRenderLayer(layer);
            try {
                for (BlockPos pos : selection) {
                    IBlockState state = world.getBlockState(pos);
                    if (!state.getBlock().canRenderInLayer(state, layer)) {
                        continue;
                    }
                    // A private builder prevents one broken third-party renderer from corrupting
                    // every block that follows it in this section.
                    BufferBuilder blockBuilder = new BufferBuilder(65536);
                    blockBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
                    try (GlStateGuard ignored = GlStateGuard.capture()) {
                        Minecraft.getMinecraft().getBlockRendererDispatcher()
                            .renderBlock(state, pos, maskedWorld, blockBuilder);
                        if (blockBuilder.getVertexCount() > 0) {
                            aggregate.addVertexData(blockBuilder.getVertexState().getRawBuffer());
                        }
                    } catch (Throwable throwable) {
                        LOGGER.warn("Block renderer failed for {} at {} in section layer {}",
                            state, pos, layer, throwable);
                    }
                }
                if (aggregate.getVertexCount() > 0) {
                    layers.put(layer, aggregate.getVertexState());
                }
            } finally {
                ForgeHooksClient.setRenderLayer(null);
            }
        }
        for (TileEntity tile : world.getTileEntities()) {
            if (selection.test(tile.getPos())) {
                tiles.add(tile);
            }
        }
        dirty = false;
    }

    public void render(BlockRenderLayer layer, Vec3d localCamera) {
        BufferBuilder.State state = layers.get(layer);
        if (state == null || state.getVertexCount() == 0) {
            return;
        }
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder target = tessellator.getBuffer();
        boolean finished = false;
        try {
            target.begin(GL11.GL_QUADS, state.getVertexFormat());
            target.setVertexState(state);
            if (layer == BlockRenderLayer.TRANSLUCENT && localCamera != null) {
                target.sortVertexData((float) localCamera.x, (float) localCamera.y, (float) localCamera.z);
            }
            tessellator.draw();
            finished = true;
        } finally {
            if (!finished) {
                finishSharedBuffer(tessellator);
            }
        }
    }

    public Collection<TileEntity> getTiles() {
        return new ArrayList<TileEntity>(tiles);
    }

    public void clear() {
        layers.clear();
        tiles.clear();
        dirty = true;
    }

    static IBlockAccess createSelectionView(PonderWorld world, Selection selection) {
        if (world == null || selection == null) {
            throw new IllegalArgumentException("A world and selection are required");
        }
        return new SelectionBlockAccess(world, selection);
    }

    boolean isDirtyForTesting() {
        return dirty;
    }

    void markCleanForTesting() {
        dirty = false;
    }

    private static void finishSharedBuffer(Tessellator tessellator) {
        try {
            tessellator.draw();
        } catch (Throwable ignored) {
            // If draw() already completed before failing, BufferBuilder correctly reports
            // "Not building" here. In either case there is no remaining state to upload.
        }
    }

    /**
     * Presents only the selected blocks to model renderers. Neighbours outside the section
     * are intentionally air so that a moving section retains all newly exposed faces.
     */
    private static final class SelectionBlockAccess implements IBlockAccess {
        private final PonderWorld delegate;
        private final Selection selection;

        private SelectionBlockAccess(PonderWorld delegate, Selection selection) {
            this.delegate = delegate;
            this.selection = selection;
        }

        @Override
        @Nullable
        public TileEntity getTileEntity(BlockPos pos) {
            return selection.test(pos) ? delegate.getTileEntity(pos) : null;
        }

        @Override
        public int getCombinedLight(BlockPos pos, int lightValue) {
            return delegate.getCombinedLight(pos, lightValue);
        }

        @Override
        public IBlockState getBlockState(BlockPos pos) {
            return selection.test(pos) ? delegate.getBlockState(pos) : Blocks.AIR.getDefaultState();
        }

        @Override
        public boolean isAirBlock(BlockPos pos) {
            IBlockState state = getBlockState(pos);
            return state.getBlock().isAir(state, this, pos);
        }

        @Override
        public Biome getBiome(BlockPos pos) {
            return delegate.getBiome(pos);
        }

        @Override
        public int getStrongPower(BlockPos pos, EnumFacing direction) {
            return selection.test(pos) ? delegate.getStrongPower(pos, direction) : 0;
        }

        @Override
        public WorldType getWorldType() {
            return delegate.getWorldType();
        }

        @Override
        public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean defaultValue) {
            return selection.test(pos) && delegate.isSideSolid(pos, side, defaultValue);
        }
    }
}
