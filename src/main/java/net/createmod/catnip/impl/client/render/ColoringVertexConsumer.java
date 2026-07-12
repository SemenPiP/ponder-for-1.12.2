package net.createmod.catnip.impl.client.render;

import net.minecraft.client.renderer.BufferBuilder;

/** Full-vertex adapter for callers that need multiplicative tint before BufferBuilder emission. */
public final class ColoringVertexConsumer {
    private final BufferBuilder delegate;private final float red,green,blue,alpha;
    public ColoringVertexConsumer(BufferBuilder delegate,float red,float green,float blue,float alpha){this.delegate=delegate;this.red=red;this.green=green;this.blue=blue;this.alpha=alpha;}
    public void vertex(double x,double y,double z,int r,int g,int b,int a,double u,double v,int lightX,int lightY,float nx,float ny,float nz){delegate.pos(x,y,z).color(Math.round(r*red),Math.round(g*green),Math.round(b*blue),Math.round(a*alpha)).tex(u,v).lightmap(lightX,lightY).normal(nx,ny,nz).endVertex();}
    public BufferBuilder delegate(){return delegate;}
}
