package net.createmod.catnip.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;

public class SuperBufferFactory {
    private static SuperBufferFactory instance = new SuperBufferFactory();

    public static SuperBufferFactory getInstance() {
        return instance;
    }

    public static void setInstance(SuperBufferFactory replacement) {
        if (replacement == null) throw new IllegalArgumentException("factory");
        instance = replacement;
    }

    public SuperByteBuffer create(BufferBuilder.State state) {
        return new DefaultSuperByteBuffer(state);
    }

    public SuperByteBuffer create(TemplateMesh mesh) {
        return new ShadeSeparatingSuperByteBuffer(mesh);
    }

    public SuperByteBuffer createForBlock(IBlockState state) {
        if (state == null) throw new IllegalArgumentException("state");
        Minecraft minecraft = Minecraft.getMinecraft();
        World world = minecraft.world;
        if (world == null) {
            throw new IllegalStateException("Cannot build a block buffer without an active client world");
        }
        return createForBlock(state, world, minecraft.getBlockRendererDispatcher());
    }

    SuperByteBuffer createForBlock(IBlockState state, World world, BlockRendererDispatcher dispatcher) {
        if (state == null) throw new IllegalArgumentException("state");
        if (world == null) {
            throw new IllegalStateException("Cannot build a block buffer without an active client world");
        }
        if (dispatcher == null) throw new IllegalArgumentException("dispatcher");

        BufferBuilder builder = new BufferBuilder(65536);
        builder.begin(org.lwjgl.opengl.GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        try {
            for (BlockRenderLayer layer : renderLayersFor(state)) {
                ForgeHooksClient.setRenderLayer(layer);
                dispatcher.renderBlock(state, BlockPos.ORIGIN, world, builder);
            }
            return create(builder.getVertexState());
        } finally {
            ForgeHooksClient.setRenderLayer(null);
        }
    }

    static List<BlockRenderLayer> renderLayersFor(IBlockState state) {
        if (state == null) throw new IllegalArgumentException("state");
        List<BlockRenderLayer> layers = new ArrayList<BlockRenderLayer>();
        for (BlockRenderLayer layer : BlockRenderLayer.values()) {
            if (state.getBlock().canRenderInLayer(state, layer)) layers.add(layer);
        }
        return Collections.unmodifiableList(layers);
    }
}
