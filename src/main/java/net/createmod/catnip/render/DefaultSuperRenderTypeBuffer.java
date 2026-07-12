package net.createmod.catnip.render;

import java.util.EnumMap;
import java.util.Map;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.BlockRenderLayer;

public class DefaultSuperRenderTypeBuffer implements SuperRenderTypeBuffer {
    private static final DefaultSuperRenderTypeBuffer INSTANCE=new DefaultSuperRenderTypeBuffer();
    private final Phase early=new Phase(),normal=new Phase(),late=new Phase();
    public static DefaultSuperRenderTypeBuffer getInstance(){return INSTANCE;}
    @Override public BufferBuilder getEarlyBuffer(BlockRenderLayer layer){return early.get(layer);}
    @Override public BufferBuilder getBuffer(BlockRenderLayer layer){return normal.get(layer);}
    @Override public BufferBuilder getLateBuffer(BlockRenderLayer layer){return late.get(layer);}
    @Override public void draw(){early.drawAll();normal.drawAll();late.drawAll();}
    @Override public void draw(BlockRenderLayer layer){early.draw(layer);normal.draw(layer);late.draw(layer);}
    private static final class Phase{
        private final Map<BlockRenderLayer,BufferBuilder> buffers=new EnumMap<BlockRenderLayer,BufferBuilder>(BlockRenderLayer.class);
        BufferBuilder get(BlockRenderLayer layer){BufferBuilder b=buffers.get(layer);if(b==null){b=new BufferBuilder(131072);b.begin(GL11.GL_QUADS,DefaultVertexFormats.BLOCK);buffers.put(layer,b);}return b;}
        void drawAll(){for(BlockRenderLayer layer:BlockRenderLayer.values())draw(layer);}
        void draw(BlockRenderLayer layer){BufferBuilder b=buffers.remove(layer);if(b==null||b.getVertexCount()==0)return;try(GlStateGuard ignored=GlStateGuard.capture()){Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);configure(layer);b.finishDrawing();new WorldVertexBufferUploader().draw(b);}}
        private static void configure(BlockRenderLayer layer){GlStateManager.enableDepth();if(layer==BlockRenderLayer.TRANSLUCENT){GlStateManager.enableBlend();GlStateManager.depthMask(false);GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);}else{GlStateManager.disableBlend();GlStateManager.depthMask(true);}}
    }
}
