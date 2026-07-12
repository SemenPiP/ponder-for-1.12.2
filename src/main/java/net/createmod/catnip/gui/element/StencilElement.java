package net.createmod.catnip.gui.element;

import org.lwjgl.opengl.GL11;
import net.createmod.catnip.render.GlStateGuard;
import net.minecraft.client.renderer.GlStateManager;

public interface StencilElement extends RenderElement {
    void renderStencil();void renderElement();
    @Override default void render(){try(GlStateGuard ignored=GlStateGuard.capture()){GlStateManager.pushMatrix();GlStateManager.translate(getX(),getY(),getZ());GL11.glEnable(GL11.GL_STENCIL_TEST);GL11.glClearStencil(0);GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);GL11.glStencilMask(0xff);GL11.glStencilFunc(GL11.GL_ALWAYS,1,0xff);GL11.glStencilOp(GL11.GL_KEEP,GL11.GL_KEEP,GL11.GL_REPLACE);GlStateManager.colorMask(false,false,false,false);renderStencil();GlStateManager.colorMask(true,true,true,true);GL11.glStencilMask(0);GL11.glStencilFunc(GL11.GL_EQUAL,1,0xff);GL11.glStencilOp(GL11.GL_KEEP,GL11.GL_KEEP,GL11.GL_KEEP);renderElement();GlStateManager.popMatrix();}}
}
