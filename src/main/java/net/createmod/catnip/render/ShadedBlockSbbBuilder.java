package net.createmod.catnip.render;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import net.createmod.catnip.client.render.model.ShadeSeparatedResultConsumer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.BlockRenderLayer;

/** Collects shade-separated geometry without discarding its 1.12 render layer. */
public class ShadedBlockSbbBuilder extends SuperByteBufferBuilder implements ShadeSeparatedResultConsumer {
    private final Map<BlockRenderLayer, SuperByteBufferBuilder> layerBuilders =
        new EnumMap<BlockRenderLayer, SuperByteBufferBuilder>(BlockRenderLayer.class);
    private final Set<BlockRenderLayer> presentLayers = EnumSet.noneOf(BlockRenderLayer.class);

    public ShadedBlockSbbBuilder() {
        for (BlockRenderLayer layer : BlockRenderLayer.values())
            layerBuilders.put(layer, new SuperByteBufferBuilder());
    }

    public static ShadedBlockSbbBuilder create() {
        return new ShadedBlockSbbBuilder();
    }

    @Override
    public void prepare() {
        super.prepare();
        presentLayers.clear();
        for (SuperByteBufferBuilder builder : layerBuilders.values()) builder.prepare();
    }

    @Override
    public void accept(BlockRenderLayer layer, boolean shaded, BufferBuilder.State state) {
        if (layer == null || state == null) throw new IllegalArgumentException("layer/state");
        presentLayers.add(layer);
        layerBuilders.get(layer).add(state, shaded);
        super.add(state, shaded);
    }

    public SuperByteBuffer build(BlockRenderLayer layer) {
        SuperByteBufferBuilder builder = layerBuilders.get(layer);
        if (builder == null) throw new IllegalArgumentException("Unknown render layer: " + layer);
        return builder.build();
    }

    public Map<BlockRenderLayer, SuperByteBuffer> buildLayers() {
        Map<BlockRenderLayer, SuperByteBuffer> result =
            new EnumMap<BlockRenderLayer, SuperByteBuffer>(BlockRenderLayer.class);
        for (BlockRenderLayer layer : presentLayers) result.put(layer, layerBuilders.get(layer).build());
        return Collections.unmodifiableMap(result);
    }

    public Set<BlockRenderLayer> getPresentLayers() {
        EnumSet<BlockRenderLayer> copy = EnumSet.noneOf(BlockRenderLayer.class);
        copy.addAll(presentLayers);
        return Collections.unmodifiableSet(copy);
    }
}
