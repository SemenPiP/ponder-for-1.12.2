package net.createmod.catnip.client.render.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL11;

import net.createmod.catnip.impl.client.render.model.DefaultShadeSeparatedBufferSource;
import net.createmod.catnip.render.PoseStack;
import net.createmod.catnip.render.ShadeSeparatingSuperByteBuffer;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.render.SuperByteBufferBuilder;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.ForgeHooksClient;

/** Buffers 1.12 baked models while retaining render-layer and diffuse-shade boundaries. */
public final class BakedModelBufferer {
    private static final int INITIAL_BUFFER_SIZE = 1 << 16;

    private BakedModelBufferer() {
    }

    public static void bufferModel(IBakedModel model, BlockPos pos, IBlockAccess level, IBlockState state,
                                   @Nullable PoseStack poseStack, ShadeSeparatedBufferSource buffers) {
        if (model == null || pos == null || level == null || state == null || buffers == null)
            throw new IllegalArgumentException("Model buffering arguments cannot be null");

        BlockRendererDispatcher dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
        for (BlockRenderLayer layer : BlockRenderLayer.values()) {
            if (!state.getBlock().canRenderInLayer(state, layer)) continue;
            ForgeHooksClient.setRenderLayer(layer);
            try {
                bufferShade(dispatcher, new ShadeFilteredModel(model, true), pos, level, state,
                    poseStack, buffers.getBuffer(layer, true), true);
                bufferShade(dispatcher, new ShadeFilteredModel(model, false), pos, level, state,
                    poseStack, buffers.getBuffer(layer, false), false);
            } finally {
                ForgeHooksClient.setRenderLayer(null);
            }
        }
    }

    public static void bufferModel(IBakedModel model, BlockPos pos, IBlockAccess level, IBlockState state,
                                   @Nullable PoseStack poseStack, ShadeSeparatedResultConsumer consumer) {
        DefaultShadeSeparatedBufferSource source = new DefaultShadeSeparatedBufferSource();
        source.prepare(consumer);
        try {
            bufferModel(model, pos, level, state, poseStack, source);
        } finally {
            source.end();
        }
    }

    public static void bufferBlocks(Iterator<BlockPos> positions, IBlockAccess level,
                                    @Nullable PoseStack poseStack, boolean renderFluids,
                                    ShadeSeparatedBufferSource buffers) {
        if (positions == null || level == null || buffers == null)
            throw new IllegalArgumentException("Block buffering arguments cannot be null");
        BlockRendererDispatcher dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
        while (positions.hasNext()) {
            BlockPos pos = positions.next();
            if (pos == null) continue;
            IBlockState state = level.getBlockState(pos);
            EnumBlockRenderType renderType = state.getRenderType();
            if (renderType == EnumBlockRenderType.MODEL) {
                IBlockState actual = state;
                try {
                    actual = state.getActualState(level, pos);
                } catch (RuntimeException ignored) {
                    // Some modded states require a full World; the supplied state remains renderable.
                }
                IBakedModel model = dispatcher.getModelForState(actual);
                IBlockState extended = actual.getBlock().getExtendedState(actual, level, pos);
                bufferModel(model, pos, level, extended, poseStack, buffers);
            } else if (renderFluids && renderType == EnumBlockRenderType.LIQUID) {
                bufferFluid(dispatcher, pos, level, state, poseStack, buffers);
            }
        }
    }

    public static void bufferBlocks(Iterator<BlockPos> positions, IBlockAccess level,
                                    @Nullable PoseStack poseStack, boolean renderFluids,
                                    ShadeSeparatedResultConsumer consumer) {
        DefaultShadeSeparatedBufferSource source = new DefaultShadeSeparatedBufferSource();
        source.prepare(consumer);
        try {
            bufferBlocks(positions, level, poseStack, renderFluids, source);
        } finally {
            source.end();
        }
    }

    private static void bufferShade(BlockRendererDispatcher dispatcher, IBakedModel model, BlockPos pos,
                                    IBlockAccess level, IBlockState state, @Nullable PoseStack poseStack,
                                    BufferBuilder target, boolean shaded) {
        BufferBuilder temporary = new BufferBuilder(INITIAL_BUFFER_SIZE);
        temporary.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        dispatcher.getBlockModelRenderer().renderModel(level, model, state, pos, temporary, true);
        flush(temporary, target, poseStack, shaded);
    }

    private static void bufferFluid(BlockRendererDispatcher dispatcher, BlockPos pos, IBlockAccess level,
                                    IBlockState state, @Nullable PoseStack poseStack,
                                    ShadeSeparatedBufferSource buffers) {
        for (BlockRenderLayer layer : BlockRenderLayer.values()) {
            if (!state.getBlock().canRenderInLayer(state, layer)) continue;
            BufferBuilder temporary = new BufferBuilder(INITIAL_BUFFER_SIZE);
            temporary.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
            ForgeHooksClient.setRenderLayer(layer);
            try {
                dispatcher.renderBlock(state, pos, level, temporary);
            } finally {
                ForgeHooksClient.setRenderLayer(null);
            }
            flush(temporary, buffers.getBuffer(layer, true), poseStack, true);
        }
    }

    private static void flush(BufferBuilder temporary, BufferBuilder target,
                              @Nullable PoseStack poseStack, boolean shaded) {
        if (temporary.getVertexCount() == 0) return;
        SuperByteBuffer buffer = new ShadeSeparatingSuperByteBuffer(
            SuperByteBufferBuilder.meshFrom(temporary.getVertexState()), shaded ? new int[0] : new int[] {0});
        try {
            if (poseStack != null) buffer.transform(poseStack.last().pose());
            buffer.renderInto(target);
        } finally {
            buffer.delete();
        }
    }

    private static final class ShadeFilteredModel implements IBakedModel {
        private final IBakedModel delegate;
        private final boolean shaded;

        private ShadeFilteredModel(IBakedModel delegate, boolean shaded) {
            this.delegate = delegate;
            this.shaded = shaded;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
            List<BakedQuad> source = delegate.getQuads(state, side, rand);
            if (source.isEmpty()) return Collections.emptyList();
            List<BakedQuad> result = new ArrayList<BakedQuad>(source.size());
            for (BakedQuad quad : source)
                if (quad.shouldApplyDiffuseLighting() == shaded) result.add(quad);
            return result;
        }

        @Override public boolean isAmbientOcclusion() { return delegate.isAmbientOcclusion(); }
        @Override public boolean isAmbientOcclusion(IBlockState state) { return delegate.isAmbientOcclusion(state); }
        @Override public boolean isGui3d() { return delegate.isGui3d(); }
        @Override public boolean isBuiltInRenderer() { return delegate.isBuiltInRenderer(); }
        @Override public TextureAtlasSprite getParticleTexture() { return delegate.getParticleTexture(); }
        @Override public ItemCameraTransforms getItemCameraTransforms() { return delegate.getItemCameraTransforms(); }
        @Override public ItemOverrideList getOverrides() { return delegate.getOverrides(); }
    }
}
