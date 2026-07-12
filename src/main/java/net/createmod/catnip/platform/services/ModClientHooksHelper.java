package net.createmod.catnip.platform.services;

import java.util.Iterator;
import java.util.Locale;

import javax.annotation.Nullable;

import net.createmod.catnip.client.render.model.ShadeSeparatedBufferSource;
import net.createmod.catnip.client.render.model.ShadeSeparatedResultConsumer;
import net.createmod.catnip.render.PoseStack;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/** Forge-version-specific client operations used by side-agnostic Catnip code. */
public interface ModClientHooksHelper {
    Locale getCurrentLocale();

    @Nullable
    Particle createParticleFromData(EnumParticleTypes type, World level,
                                    double x, double y, double z,
                                    double motionX, double motionY, double motionZ,
                                    int... parameters);

    Minecraft getMinecraftFromScreen(@Nullable GuiScreen screen);

    boolean isKeyPressed(KeyBinding mapping);

    void enableStencilBuffer(Framebuffer framebuffer);

    boolean renderFullFluidState(IBlockAccess level, IBlockState state, BlockPos pos, BufferBuilder buffer);

    void bufferModel(IBakedModel model, BlockPos pos, IBlockAccess level, IBlockState state,
                     @Nullable PoseStack poseStack, ShadeSeparatedBufferSource bufferSource);

    void bufferModel(IBakedModel model, BlockPos pos, IBlockAccess level, IBlockState state,
                     @Nullable PoseStack poseStack, ShadeSeparatedResultConsumer resultConsumer);

    void bufferBlocks(Iterator<BlockPos> positions, IBlockAccess level, @Nullable PoseStack poseStack,
                      boolean renderFluids, ShadeSeparatedBufferSource bufferSource);

    void bufferBlocks(Iterator<BlockPos> positions, IBlockAccess level, @Nullable PoseStack poseStack,
                      boolean renderFluids, ShadeSeparatedResultConsumer resultConsumer);

    Iterable<BlockRenderLayer> getRenderLayersForBlockModel(IBlockState state);

    boolean doesBlockModelContainRenderLayer(BlockRenderLayer layer, IBlockState state);

    BlockRendererDispatcher getBlockRenderDispatcher();
}
