package net.createmod.catnip.outliner;

import org.lwjgl.opengl.GL11;

import net.createmod.catnip.render.GlStateGuard;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

public class AABBOutline extends Outline {
    protected AxisAlignedBB bounds;

    public AABBOutline(AxisAlignedBB bounds) { this.bounds = bounds; }
    public AxisAlignedBB getBounds() { return bounds; }
    public void setBounds(AxisAlignedBB bounds) { this.bounds = bounds; }

    @Override
    public void render(Vec3d camera, float partialTicks) {
        double minX = bounds.minX - camera.x;
        double minY = bounds.minY - camera.y;
        double minZ = bounds.minZ - camera.z;
        double maxX = bounds.maxX - camera.x;
        double maxY = bounds.maxY - camera.y;
        double maxZ = bounds.maxZ - camera.z;
        int color = params.getColor();
        int alpha = Math.round((color >>> 24) * params.getAlpha());
        try (GlStateGuard ignored = GlStateGuard.capture()) {
            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.enableBlend();
            GlStateManager.depthMask(false);
            if (params.getFaceTexture() != null) {
                drawFaces(minX, minY, minZ, maxX, maxY, maxZ, color, alpha / 4);
            }
            GL11.glLineWidth(Math.max(1, params.getLineWidth() * 16));
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            edge(buffer, minX,minY,minZ, maxX,minY,minZ,color,alpha);
            edge(buffer, minX,minY,minZ, minX,maxY,minZ,color,alpha);
            edge(buffer, minX,minY,minZ, minX,minY,maxZ,color,alpha);
            edge(buffer, maxX,maxY,maxZ, minX,maxY,maxZ,color,alpha);
            edge(buffer, maxX,maxY,maxZ, maxX,minY,maxZ,color,alpha);
            edge(buffer, maxX,maxY,maxZ, maxX,maxY,minZ,color,alpha);
            edge(buffer, maxX,minY,minZ, maxX,maxY,minZ,color,alpha);
            edge(buffer, maxX,minY,minZ, maxX,minY,maxZ,color,alpha);
            edge(buffer, minX,maxY,minZ, maxX,maxY,minZ,color,alpha);
            edge(buffer, minX,maxY,minZ, minX,maxY,maxZ,color,alpha);
            edge(buffer, minX,minY,maxZ, maxX,minY,maxZ,color,alpha);
            edge(buffer, minX,minY,maxZ, minX,maxY,maxZ,color,alpha);
            tessellator.draw();
        }
    }

    private static void edge(BufferBuilder b, double x1,double y1,double z1,double x2,double y2,double z2,
                             int c,int a) {
        vertex(b,x1,y1,z1,c,a); vertex(b,x2,y2,z2,c,a);
    }
    private static void vertex(BufferBuilder b,double x,double y,double z,int c,int a) {
        b.pos(x,y,z).color(c>>16&255,c>>8&255,c&255,a).endVertex();
    }
    private static void drawFaces(double x0,double y0,double z0,double x1,double y1,double z1,int c,int a) {
        Tessellator t=Tessellator.getInstance(); BufferBuilder b=t.getBuffer();
        b.begin(GL11.GL_QUADS,DefaultVertexFormats.POSITION_COLOR);
        quad(b,x0,y0,z0,x1,y0,z0,x1,y0,z1,x0,y0,z1,c,a);
        quad(b,x0,y1,z1,x1,y1,z1,x1,y1,z0,x0,y1,z0,c,a);
        quad(b,x0,y0,z0,x0,y0,z1,x0,y1,z1,x0,y1,z0,c,a);
        quad(b,x1,y0,z1,x1,y0,z0,x1,y1,z0,x1,y1,z1,c,a);
        quad(b,x1,y0,z0,x0,y0,z0,x0,y1,z0,x1,y1,z0,c,a);
        quad(b,x0,y0,z1,x1,y0,z1,x1,y1,z1,x0,y1,z1,c,a); t.draw();
    }
    private static void quad(BufferBuilder b,double ax,double ay,double az,double bx,double by,double bz,
                             double cx,double cy,double cz,double dx,double dy,double dz,int c,int a) {
        vertex(b,ax,ay,az,c,a); vertex(b,bx,by,bz,c,a); vertex(b,cx,cy,cz,c,a); vertex(b,dx,dy,dz,c,a);
    }
}
