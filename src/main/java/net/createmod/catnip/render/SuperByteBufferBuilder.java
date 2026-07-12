package net.createmod.catnip.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.VertexFormat;

public class SuperByteBufferBuilder {
    protected final MutableTemplateMesh mesh=new MutableTemplateMesh();
    protected final List<Integer> shadeSwaps=new ArrayList<Integer>();
    private boolean shade=true;
    public void prepare(){mesh.clear();shadeSwaps.clear();shade=true;}
    public void add(BufferBuilder.State state,boolean shaded){if(shaded!=shade){shadeSwaps.add(mesh.vertexCount());shade=shaded;}mesh.append(meshFrom(state));}
    public SuperByteBuffer build(){int[] swaps=new int[shadeSwaps.size()];for(int i=0;i<swaps.length;i++)swaps[i]=shadeSwaps.get(i);return new ShadeSeparatingSuperByteBuffer(mesh.toImmutable(),swaps);}

    public static TemplateMesh meshFrom(BufferBuilder.State state){
        VertexFormat format=state.getVertexFormat();int stride=format.getIntegerSize();int[] raw=state.getRawBuffer();
        ByteBuffer bytes=ByteBuffer.allocate(raw.length*4).order(ByteOrder.nativeOrder());bytes.asIntBuffer().put(raw);
        MutableTemplateMesh result=new MutableTemplateMesh(state.getVertexCount());
        int colorOffset=format.hasColor()?format.getColorOffset():-1;int uv0=format.hasUvOffset(0)?format.getUvOffsetById(0):-1;int uv1=format.hasUvOffset(1)?format.getUvOffsetById(1):-1;int normalOffset=format.hasNormal()?format.getNormalOffset():-1;
        for(int i=0;i<state.getVertexCount();i++){int base=i*stride*4;float x=bytes.getFloat(base),y=bytes.getFloat(base+4),z=bytes.getFloat(base+8);int color=0xffffffff;
            if(colorOffset>=0){int r=bytes.get(base+colorOffset)&255,g=bytes.get(base+colorOffset+1)&255,b=bytes.get(base+colorOffset+2)&255,a=bytes.get(base+colorOffset+3)&255;color=a<<24|r<<16|g<<8|b;}
            float u=uv0<0?0:bytes.getFloat(base+uv0),v=uv0<0?0:bytes.getFloat(base+uv0+4);int light=uv1<0?0x00f000f0:(bytes.getShort(base+uv1)&0xffff)|((bytes.getShort(base+uv1+2)&0xffff)<<16);int normal=MutableTemplateMesh.packNormal(0,1,0);
            if(normalOffset>=0)normal=(bytes.get(base+normalOffset)&255)|(bytes.get(base+normalOffset+1)&255)<<8|(bytes.get(base+normalOffset+2)&255)<<16;
            result.add(x,y,z,color,u,v,light,(byte)normal/127f,(byte)(normal>>8)/127f,(byte)(normal>>16)/127f);
        }return result.toImmutable();
    }
}
