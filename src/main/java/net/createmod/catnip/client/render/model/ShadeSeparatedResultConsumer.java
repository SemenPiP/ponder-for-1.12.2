package net.createmod.catnip.client.render.model;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.BlockRenderLayer;

public interface ShadeSeparatedResultConsumer {
    void accept(BlockRenderLayer layer,boolean shaded,BufferBuilder.State state);
}
