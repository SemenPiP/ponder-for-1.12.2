package net.createmod.catnip.gui;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class UIRenderHelper {
    private UIRenderHelper() {}

    public static void coloredQuad(int x, int y, int width, int height, int argb) {
        Gui.drawRect(x, y, x + width, y + height, argb);
    }

    public static void texturedQuad(TextureSheetSegment segment, int x, int y) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(segment.getTextureLocation());
        float u0 = segment.getStartX() / (float) segment.getSheetWidth();
        float v0 = segment.getStartY() / (float) segment.getSheetHeight();
        float u1 = (segment.getStartX() + segment.getWidth()) / (float) segment.getSheetWidth();
        float v1 = (segment.getStartY() + segment.getHeight()) / (float) segment.getSheetHeight();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(x, y + segment.getHeight(), 0).tex(u0, v1).endVertex();
        buffer.pos(x + segment.getWidth(), y + segment.getHeight(), 0).tex(u1, v1).endVertex();
        buffer.pos(x + segment.getWidth(), y, 0).tex(u1, v0).endVertex();
        buffer.pos(x, y, 0).tex(u0, v0).endVertex();
        tessellator.draw();
    }

    public static void enableScissor(int x, int y, int width, int height) {
        net.minecraft.client.gui.ScaledResolution resolution =
            new net.minecraft.client.gui.ScaledResolution(Minecraft.getMinecraft());
        int scale = resolution.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scale, Minecraft.getMinecraft().displayHeight - (y + height) * scale,
            Math.max(0, width * scale), Math.max(0, height * scale));
    }

    public static void disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public static void resetColor() {
        GlStateManager.color(1f, 1f, 1f, 1f);
    }
}
