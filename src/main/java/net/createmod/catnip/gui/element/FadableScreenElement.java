package net.createmod.catnip.gui.element;

public interface FadableScreenElement extends ScreenElement {
    @Override void render(int x,int y,float alpha);
    @Override default void render(int x,int y){render(x,y,1);}
}
