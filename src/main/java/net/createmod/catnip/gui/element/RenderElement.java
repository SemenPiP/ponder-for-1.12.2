package net.createmod.catnip.gui.element;

public interface RenderElement extends FadableScreenElement {
    static RenderElement of(ScreenElement element){return new AbstractRenderElement.SimpleRenderElement(element);}
    <T extends RenderElement>T at(float x,float y);
    <T extends RenderElement>T at(float x,float y,float z);
    <T extends RenderElement>T withBounds(int width,int height);
    <T extends RenderElement>T withAlpha(float alpha);
    int getWidth();int getHeight();float getX();float getY();float getZ();
    void render();
    @Override default void render(int x,int y,float alpha){at(x,y).withAlpha(alpha).render();}
}
