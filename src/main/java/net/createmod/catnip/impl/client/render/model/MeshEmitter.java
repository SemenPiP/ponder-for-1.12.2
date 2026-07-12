package net.createmod.catnip.impl.client.render.model;

import org.lwjgl.opengl.GL11;
import net.createmod.catnip.client.render.model.ShadeSeparatedResultConsumer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.BlockRenderLayer;

public final class MeshEmitter {
    private final BlockRenderLayer layer;
    private BufferBuilder builder;
    private boolean shade;
    private ShadeSeparatedResultConsumer consumer;
    public MeshEmitter(BlockRenderLayer layer){this.layer=layer;}
    public void prepare(ShadeSeparatedResultConsumer consumer){this.consumer=consumer;}
    public BufferBuilder getBuffer(boolean shaded){if(builder==null||shade!=shaded){emit();shade=shaded;builder=new BufferBuilder(65536);builder.begin(GL11.GL_QUADS,DefaultVertexFormats.BLOCK);}return builder;}
    public void end(){emit();consumer=null;}
    private void emit(){if(builder!=null&&builder.getVertexCount()>0&&consumer!=null)consumer.accept(layer,shade,builder.getVertexState());builder=null;}
}
