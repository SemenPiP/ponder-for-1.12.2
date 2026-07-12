package net.createmod.catnip.client.render.model;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.BlockRenderLayer;

public interface ShadeSeparatedBufferSource {
    BufferBuilder getBuffer(BlockRenderLayer layer,boolean shade);
}
