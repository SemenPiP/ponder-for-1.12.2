package net.createmod.catnip.gui.element;

import net.createmod.catnip.data.Couple;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.gui.Gui;

public class BoxElement extends AbstractRenderElement {
    public static final Couple<Color> COLOR_VANILLA_BORDER=Couple.create(new Color(0x505000ff),new Color(0x5028007f));
    public static final Color COLOR_VANILLA_BACKGROUND=new Color(0xf0100010);
    public static final Color COLOR_BACKGROUND_FLAT=new Color(0xff000000);
    public static final Color COLOR_BACKGROUND_TRANSPARENT=new Color(0xdd000000);
    private Color background=COLOR_VANILLA_BACKGROUND,borderTop=COLOR_VANILLA_BORDER.getFirst(),borderBottom=COLOR_VANILLA_BORDER.getSecond();private int borderOffset=2;
    public BoxElement withBackground(Color color){background=color;return this;}public BoxElement withBackground(int color){return withBackground(new Color(color));}
    public BoxElement flatBorder(Color color){borderTop=borderBottom=color;return this;}public BoxElement flatBorder(int color){return flatBorder(new Color(color));}
    public BoxElement gradientBorder(Couple<Color> colors){borderTop=colors.getFirst();borderBottom=colors.getSecond();return this;}public BoxElement gradientBorder(Color top,Color bottom){borderTop=top;borderBottom=bottom;return this;}public BoxElement gradientBorder(int top,int bottom){return gradientBorder(new Color(top),new Color(bottom));}
    public BoxElement withBorderOffset(int offset){borderOffset=Math.max(0,offset);return this;}
    @Override public void render(){int left=Math.round(x)-borderOffset-1,top=Math.round(y)-borderOffset-1,right=Math.round(x)+width+borderOffset+1,bottom=Math.round(y)+height+borderOffset+1;Gui.drawRect(left,top,right,bottom,scaled(background));Gui.drawRect(left,top,right,top+1,scaled(borderTop));Gui.drawRect(left,top,left+1,bottom,scaled(borderTop));Gui.drawRect(left,bottom-1,right,bottom,scaled(borderBottom));Gui.drawRect(right-1,top,right,bottom,scaled(borderBottom));}
    private int scaled(Color color){return color.copy().scaleAlpha(alpha).getRGB();}
}
