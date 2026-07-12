package net.createmod.catnip.platform;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import net.createmod.catnip.client.render.model.BakedModelBufferer;
import net.createmod.catnip.client.render.model.ShadeSeparatedBufferSource;
import net.createmod.catnip.client.render.model.ShadeSeparatedResultConsumer;
import net.createmod.catnip.impl.client.render.model.DefaultShadeSeparatedBufferSource;
import net.createmod.catnip.platform.services.ModClientHooksHelper;
import net.createmod.catnip.render.PoseStack;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Forge 14.23 client delegation without modern rendering-library dependencies. */
@SideOnly(Side.CLIENT)
public final class ForgeClientHooksHelper implements ModClientHooksHelper {
    @Override
    public Locale getCurrentLocale() {
        Locale locale = MinecraftForgeClient.getLocale();
        return locale == null ? Locale.ROOT : locale;
    }

    @Nullable
    @Override
    public Particle createParticleFromData(EnumParticleTypes type, World level,
                                           double x, double y, double z,
                                           double motionX, double motionY, double motionZ,
                                           int... parameters) {
        if (type == null || level == null) return null;
        int[] safeParameters = parameters == null ? new int[0] : parameters.clone();
        ParticleManager isolatedManager = new ParticleManager(level, Minecraft.getMinecraft().getTextureManager());
        return isolatedManager.spawnEffectParticle(type.getParticleID(), x, y, z,
            motionX, motionY, motionZ, safeParameters);
    }

    @Override
    public Minecraft getMinecraftFromScreen(@Nullable GuiScreen screen) {
        return Minecraft.getMinecraft();
    }

    @Override
    public boolean isKeyPressed(KeyBinding mapping) {
        if (mapping == null) return false;
        int keyCode = mapping.getKeyCode();
        if (keyCode == Keyboard.KEY_NONE) return false;
        return keyCode < 0 ? Mouse.isButtonDown(keyCode + 100) : Keyboard.isKeyDown(keyCode);
    }

    @Override
    public void enableStencilBuffer(Framebuffer framebuffer) {
        if (framebuffer != null && !framebuffer.isStencilEnabled()) framebuffer.enableStencil();
    }

    @Override
    public boolean renderFullFluidState(IBlockAccess level, IBlockState state, BlockPos pos,
                                        BufferBuilder buffer) {
        if (level == null || state == null || pos == null || buffer == null
            || state.getRenderType() != EnumBlockRenderType.LIQUID) return false;
        return getBlockRenderDispatcher().renderBlock(state, pos, level, buffer);
    }

    @Override
    public void bufferModel(IBakedModel model, BlockPos pos, IBlockAccess level, IBlockState state,
                            @Nullable PoseStack poseStack, ShadeSeparatedBufferSource bufferSource) {
        BakedModelBufferer.bufferModel(model, pos, level, state, poseStack, bufferSource);
    }

    @Override
    public void bufferModel(IBakedModel model, BlockPos pos, IBlockAccess level, IBlockState state,
                            @Nullable PoseStack poseStack, ShadeSeparatedResultConsumer resultConsumer) {
        BakedModelBufferer.bufferModel(model, pos, level, state, poseStack, resultConsumer);
    }

    @Override
    public void bufferBlocks(Iterator<BlockPos> positions, IBlockAccess level, @Nullable PoseStack poseStack,
                             boolean renderFluids, ShadeSeparatedBufferSource bufferSource) {
        if (positions == null || level == null || bufferSource == null) return;
        while (positions.hasNext()) {
            BlockPos pos = positions.next();
            if (pos == null) continue;
            IBlockState state = level.getBlockState(pos);
            if (state.getRenderType() == EnumBlockRenderType.INVISIBLE) continue;
            if (state.getRenderType() == EnumBlockRenderType.LIQUID) {
                if (renderFluids) bufferFluid(level, state, pos, bufferSource);
                continue;
            }
            IBakedModel model = getBlockRenderDispatcher().getModelForState(state);
            bufferModel(model, pos, level, state, poseStack, bufferSource);
        }
    }

    @Override
    public void bufferBlocks(Iterator<BlockPos> positions, IBlockAccess level, @Nullable PoseStack poseStack,
                             boolean renderFluids, ShadeSeparatedResultConsumer resultConsumer) {
        DefaultShadeSeparatedBufferSource source = new DefaultShadeSeparatedBufferSource();
        source.prepare(resultConsumer);
        try {
            bufferBlocks(positions, level, poseStack, renderFluids, source);
        } finally {
            source.end();
        }
    }

    private void bufferFluid(IBlockAccess level, IBlockState state, BlockPos pos,
                             ShadeSeparatedBufferSource bufferSource) {
        for (BlockRenderLayer layer : BlockRenderLayer.values()) {
            if (!state.getBlock().canRenderInLayer(state, layer)) continue;
            ForgeHooksClient.setRenderLayer(layer);
            try {
                renderFullFluidState(level, state, pos, bufferSource.getBuffer(layer, true));
            } finally {
                ForgeHooksClient.setRenderLayer(null);
            }
        }
    }

    @Override
    public Iterable<BlockRenderLayer> getRenderLayersForBlockModel(IBlockState state) {
        List<BlockRenderLayer> result = new ArrayList<BlockRenderLayer>();
        if (state != null) {
            for (BlockRenderLayer layer : BlockRenderLayer.values()) {
                if (state.getBlock().canRenderInLayer(state, layer)) result.add(layer);
            }
        }
        return result;
    }

    @Override
    public boolean doesBlockModelContainRenderLayer(BlockRenderLayer layer, IBlockState state) {
        return layer != null && state != null && state.getBlock().canRenderInLayer(state, layer);
    }

    @Override
    public BlockRendererDispatcher getBlockRenderDispatcher() {
        return Minecraft.getMinecraft().getBlockRendererDispatcher();
    }
}
