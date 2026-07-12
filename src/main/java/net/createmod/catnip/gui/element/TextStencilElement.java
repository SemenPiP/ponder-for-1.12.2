package net.createmod.catnip.gui.element;

import net.minecraft.client.gui.FontRenderer;

public class TextStencilElement extends DelegatedStencilElement {
    private final FontRenderer font;private String text="";private boolean centerVertical,centerHorizontal;
    public TextStencilElement(FontRenderer font){this.font=font;height=10;}public TextStencilElement(FontRenderer font,String text){this(font);this.text=text==null?"":text;}
    public TextStencilElement withText(String text){this.text=text==null?"":text;return this;}public TextStencilElement centered(boolean vertical,boolean horizontal){centerVertical=vertical;centerHorizontal=horizontal;return this;}
    @Override public void renderStencil(){int px=centerHorizontal?width/2-font.getStringWidth(text)/2:0,py=centerVertical?height/2-font.FONT_HEIGHT/2:0;font.drawString(text,px,py,0xff000000);}
    @Override public void renderElement(){int px=centerHorizontal?width/2-font.getStringWidth(text)/2:0,py=centerVertical?height/2-font.FONT_HEIGHT/2:0;net.minecraft.client.renderer.GlStateManager.pushMatrix();net.minecraft.client.renderer.GlStateManager.translate(px,py,0);element.render(font.getStringWidth(text),font.FONT_HEIGHT+2,alpha);net.minecraft.client.renderer.GlStateManager.popMatrix();}
    public String getText(){return text;}
}
