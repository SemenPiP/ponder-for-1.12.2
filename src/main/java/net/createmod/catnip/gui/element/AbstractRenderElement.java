package net.createmod.catnip.gui.element;

public abstract class AbstractRenderElement implements RenderElement {
    protected int width=16,height=16;protected float x,y,z,alpha=1;
    @SuppressWarnings("unchecked") @Override public <T extends RenderElement>T at(float x,float y){this.x=x;this.y=y;return(T)this;}
    @SuppressWarnings("unchecked") @Override public <T extends RenderElement>T at(float x,float y,float z){this.x=x;this.y=y;this.z=z;return(T)this;}
    @SuppressWarnings("unchecked") @Override public <T extends RenderElement>T withBounds(int width,int height){this.width=Math.max(0,width);this.height=Math.max(0,height);return(T)this;}
    @SuppressWarnings("unchecked") @Override public <T extends RenderElement>T withAlpha(float alpha){this.alpha=Math.max(0,Math.min(1,alpha));return(T)this;}
    @Override public int getWidth(){return width;}@Override public int getHeight(){return height;}@Override public float getX(){return x;}@Override public float getY(){return y;}@Override public float getZ(){return z;}
    public static final class SimpleRenderElement extends AbstractRenderElement {private final ScreenElement element;SimpleRenderElement(ScreenElement element){if(element==null)throw new IllegalArgumentException("element");this.element=element;}@Override public void render(){element.render(Math.round(x),Math.round(y),alpha);}}
}
