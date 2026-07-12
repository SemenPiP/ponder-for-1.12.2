package net.createmod.catnip.gui.element;

import net.createmod.catnip.theme.Color;
import net.minecraft.client.gui.Gui;

public class DelegatedStencilElement extends AbstractRenderElement implements StencilElement {
    protected FadableScreenElement stencil=new FadableScreenElement(){@Override public void render(int x,int y,float alpha){Gui.drawRect(0,0,x,y,0xffffffff);}};
    protected FadableScreenElement element=new FadableScreenElement(){@Override public void render(int x,int y,float alpha){Gui.drawRect(0,0,x,y,new Color(0xff10aacc).scaleAlpha(alpha).getRGB());}};
    public DelegatedStencilElement(){}public DelegatedStencilElement(FadableScreenElement stencil,FadableScreenElement element){this.stencil=stencil;this.element=element;}
    public DelegatedStencilElement withStencilRenderer(FadableScreenElement renderer){stencil=renderer;return this;}public DelegatedStencilElement withElementRenderer(FadableScreenElement renderer){element=renderer;return this;}
    @Override public void renderStencil(){stencil.render(width,height,1);}@Override public void renderElement(){element.render(width,height,alpha);}
}
