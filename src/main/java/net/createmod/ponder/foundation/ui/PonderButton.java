package net.createmod.ponder.foundation.ui;

import javax.annotation.Nullable;

import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.render.GlStateGuard;
import net.createmod.ponder.foundation.PonderTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;

public class PonderButton extends GuiButton {
    public enum Icon { LEFT, RIGHT, PAUSE, PLAY, REPLAY, CLOSE, INDEX, TAGS, IDENTIFY }
    private Icon icon;
    private ItemStack item=ItemStack.EMPTY;
    private PonderTag tag;
    private Runnable callback;
    private boolean active;
    private String tooltipKey;

    public PonderButton(int id,int x,int y,int width,int height,Icon icon){super(id,x,y,width,height,"");this.icon=icon;}
    public PonderButton showing(ItemStack stack){item=stack==null?ItemStack.EMPTY:stack.copy();tag=null;return this;}
    public PonderButton showing(PonderTag value){tag=value;item=value==null?ItemStack.EMPTY:value.getItemIcon();return this;}
    public PonderButton withCallback(Runnable value){callback=value;return this;}
    public PonderButton active(boolean value){active=value;return this;}
    public PonderButton withTooltip(String value){tooltipKey=value;return this;}
    public ItemStack getItem(){return item.copy();}
    @Nullable public PonderTag getTag(){return tag;}
    @Nullable public String getTooltipKey(){return tooltipKey;}
    public boolean isHoveredButton(){return hovered;}

    @Override public boolean mousePressed(Minecraft mc,int mouseX,int mouseY){
        boolean pressed=super.mousePressed(mc,mouseX,mouseY);if(pressed&&callback!=null)callback.run();return pressed;
    }

    @Override public void drawButton(Minecraft mc,int mouseX,int mouseY,float partialTicks){
        if(!visible)return;hovered=mouseX>=x&&mouseY>=y&&mouseX<x+width&&mouseY<y+height;
        int background=!enabled?0xff20242a:active?0xff31525a:hovered?0xff3b4652:0xff292f36;
        int border=active?0xff9fd6d2:hovered?0xff9fb5c9:0xff58636e;
        Gui.drawRect(x,y,x+width,y+height,background);Gui.drawRect(x,y,x+width,y+1,border);Gui.drawRect(x,y+height-1,x+width,y+height,border);
        Gui.drawRect(x,y,x+1,y+height,border);Gui.drawRect(x+width-1,y,x+width,y+height,border);
        if(!item.isEmpty()){GuiGameElement.of(item).render(x+width/2,y+height/2,enabled?1:.35f);return;}
        try(GlStateGuard ignored=GlStateGuard.capture()){
            GlStateManager.disableTexture2D();int c=enabled?0xffe3e8ed:0xff737b84;
            drawIcon(icon,x+width/2,y+height/2,c);
        }
    }

    private static void drawIcon(Icon icon,int cx,int cy,int color){
        if(icon==null)return;
        switch(icon){
            case LEFT: triangle(cx+3,cy-5,cx-4,cy,cx+3,cy+5,color);break;
            case RIGHT: triangle(cx-3,cy-5,cx+4,cy,cx-3,cy+5,color);break;
            case PLAY: triangle(cx-3,cy-5,cx+5,cy,cx-3,cy+5,color);break;
            case PAUSE: Gui.drawRect(cx-4,cy-5,cx-1,cy+5,color);Gui.drawRect(cx+2,cy-5,cx+5,cy+5,color);break;
            case REPLAY: Gui.drawRect(cx-5,cy-4,cx+4,cy-2,color);Gui.drawRect(cx+2,cy-4,cx+5,cy+4,color);Gui.drawRect(cx-4,cy+2,cx+4,cy+4,color);triangle(cx-5,cy-6,cx-5,cy,cx-1,cy-3,color);break;
            case CLOSE: Gui.drawRect(cx-5,cy-1,cx+6,cy+2,color);break;
            case INDEX: for(int i=0;i<3;i++){Gui.drawRect(cx-5,cy-5+i*5,cx-2,cy-2+i*5,color);Gui.drawRect(cx,cy-5+i*5,cx+6,cy-2+i*5,color);}break;
            case TAGS: Gui.drawRect(cx-5,cy-4,cx+5,cy+4,color);Gui.drawRect(cx+3,cy-2,cx+7,cy+2,color);break;
            case IDENTIFY: magnifier(cx,cy,color);break;
        }
    }
    private static void magnifier(int cx,int cy,int color){
        float a=(color>>>24)/255f,r=(color>>16&255)/255f,g=(color>>8&255)/255f,b=(color&255)/255f;
        org.lwjgl.opengl.GL11.glColor4f(r,g,b,a);org.lwjgl.opengl.GL11.glLineWidth(2);
        org.lwjgl.opengl.GL11.glBegin(org.lwjgl.opengl.GL11.GL_LINE_LOOP);
        for(int i=0;i<12;i++){double angle=Math.PI*2*i/12;org.lwjgl.opengl.GL11.glVertex2d(cx-1+Math.cos(angle)*4,cy-1+Math.sin(angle)*4);}
        org.lwjgl.opengl.GL11.glEnd();org.lwjgl.opengl.GL11.glBegin(org.lwjgl.opengl.GL11.GL_LINES);
        org.lwjgl.opengl.GL11.glVertex2f(cx+2,cy+2);org.lwjgl.opengl.GL11.glVertex2f(cx+6,cy+6);
        org.lwjgl.opengl.GL11.glEnd();org.lwjgl.opengl.GL11.glLineWidth(1);
    }
    private static void triangle(int ax,int ay,int bx,int by,int cx,int cy,int color){
        org.lwjgl.opengl.GL11.glBegin(org.lwjgl.opengl.GL11.GL_TRIANGLES);float a=(color>>>24)/255f,r=(color>>16&255)/255f,g=(color>>8&255)/255f,b=(color&255)/255f;
        org.lwjgl.opengl.GL11.glColor4f(r,g,b,a);org.lwjgl.opengl.GL11.glVertex2f(ax,ay);org.lwjgl.opengl.GL11.glVertex2f(bx,by);org.lwjgl.opengl.GL11.glVertex2f(cx,cy);org.lwjgl.opengl.GL11.glEnd();
    }
}
