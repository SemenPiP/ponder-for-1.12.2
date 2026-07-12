package net.createmod.catnip.impl.client.render;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import net.createmod.catnip.render.PoseStack;
import net.minecraft.client.renderer.BufferBuilder;

public final class TransformingVertexConsumer {
    private BufferBuilder delegate;private PoseStack poses;
    public void prepare(BufferBuilder delegate,PoseStack poses){this.delegate=delegate;this.poses=poses;}
    public void clear(){delegate=null;poses=null;}
    public void vertex(float x,float y,float z,int r,int g,int b,int a,float u,float v,int lightX,int lightY,float nx,float ny,float nz){if(delegate==null||poses==null)throw new IllegalStateException("Consumer is not prepared");Point3f point=new Point3f(x,y,z);poses.last().pose().transform(point);Vector3f normal=new Vector3f(nx,ny,nz);poses.last().normal().transform(normal);delegate.pos(point.x,point.y,point.z).color(r,g,b,a).tex(u,v).lightmap(lightX,lightY).normal(normal.x,normal.y,normal.z).endVertex();}
}
