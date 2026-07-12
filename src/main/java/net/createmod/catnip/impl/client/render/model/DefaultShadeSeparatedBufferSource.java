package net.createmod.catnip.impl.client.render.model;

import java.util.EnumMap;
import java.util.Map;
import net.createmod.catnip.client.render.model.ShadeSeparatedBufferSource;
import net.createmod.catnip.client.render.model.ShadeSeparatedResultConsumer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.BlockRenderLayer;

public final class DefaultShadeSeparatedBufferSource implements ShadeSeparatedBufferSource {
    private final Map<BlockRenderLayer,MeshEmitter> emitters=new EnumMap<BlockRenderLayer,MeshEmitter>(BlockRenderLayer.class);
    public DefaultShadeSeparatedBufferSource(){for(BlockRenderLayer layer:BlockRenderLayer.values())emitters.put(layer,new MeshEmitter(layer));}
    public void prepare(ShadeSeparatedResultConsumer consumer){for(MeshEmitter emitter:emitters.values())emitter.prepare(consumer);}
    public void end(){for(MeshEmitter emitter:emitters.values())emitter.end();}
    @Override public BufferBuilder getBuffer(BlockRenderLayer layer,boolean shade){return emitters.get(layer).getBuffer(shade);}
}
