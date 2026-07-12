package net.createmod.ponder.mixin;

import java.nio.ByteBuffer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.VertexFormat;

@Mixin(BufferBuilder.class)
public interface BufferBuilderAccessor {
    @Accessor("byteBuffer")
    ByteBuffer ponder$getByteBuffer();

    @Accessor("vertexCount")
    int ponder$getVertexCount();

    @Accessor("vertexFormat")
    VertexFormat ponder$getVertexFormat();
}
