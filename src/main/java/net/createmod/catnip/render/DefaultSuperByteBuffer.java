package net.createmod.catnip.render;

import net.minecraft.client.renderer.BufferBuilder;

public class DefaultSuperByteBuffer extends SuperByteBuffer {
    public DefaultSuperByteBuffer(TemplateMesh mesh){super(mesh);}
    public DefaultSuperByteBuffer(BufferBuilder.State state){super(SuperByteBufferBuilder.meshFrom(state));}
}
