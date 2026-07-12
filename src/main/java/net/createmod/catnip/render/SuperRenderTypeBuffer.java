package net.createmod.catnip.render;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.BlockRenderLayer;

public interface SuperRenderTypeBuffer {
    BufferBuilder getEarlyBuffer(BlockRenderLayer layer);
    BufferBuilder getBuffer(BlockRenderLayer layer);
    BufferBuilder getLateBuffer(BlockRenderLayer layer);
    void draw();
    void draw(BlockRenderLayer layer);
}
