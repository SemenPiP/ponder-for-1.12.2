package net.createmod.ponder.foundation.element;

import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.gui.element.ScreenElement;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.InputElementBuilder;
import net.createmod.ponder.api.element.PonderOverlayElement;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.render.SceneProjection;
import net.createmod.ponder.render.SceneProjection.ProjectedPoint;
import net.minecraft.client.gui.Gui;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

public class InputWindowElement extends PonderElementBase implements InputElementBuilder,PonderOverlayElement {
    private final Vec3d location;
    private final Pointing direction;
    private final int duration;
    private ItemStack stack=ItemStack.EMPTY;
    private ScreenElement icon;
    private int input;
    private boolean sneaking;
    private boolean control;
    public InputWindowElement(PonderScene scene,Vec3d location,Pointing direction,int duration){this.location=location;this.direction=direction;this.duration=Math.max(1,duration);}
    @Override public InputElementBuilder withItem(ItemStack value){stack=value==null?ItemStack.EMPTY:value.copy();return this;}
    @Override public InputElementBuilder leftClick(){input=1;return this;}
    @Override public InputElementBuilder rightClick(){input=2;return this;}
    @Override public InputElementBuilder scroll(){input=3;return this;}
    @Override public InputElementBuilder showing(ScreenElement value){icon=value;return this;}
    @Override public InputElementBuilder whileSneaking(){sneaking=true;return this;}
    @Override public InputElementBuilder whileCTRL(){control=true;return this;}
    @Override public void render(PonderScene scene,int mouseX,int mouseY,float partialTicks){
        ProjectedPoint point=SceneProjection.project(location);if(!point.visible)return;
        int x=(int)point.x-12,y=(int)point.y-12;Gui.drawRect(x,y,x+24,y+24,0xdd15191d);Gui.drawRect(x,y,x+24,y+1,0xff7fcde0);
        if(icon!=null)icon.render(x+12,y+12);else if(!stack.isEmpty())GuiGameElement.of(stack).render(x+12,y+12);
        int marker=input==1?0xffff6666:input==2?0xff66ccff:input==3?0xffffdd66:0xffaaaaaa;
        Gui.drawRect(x+18,y+18,x+22,y+22,marker);
        if(sneaking)Gui.drawRect(x+2,y+19,x+7,y+22,0xff88cc88);if(control)Gui.drawRect(x+8,y+19,x+13,y+22,0xffcc88cc);
    }
    public int getDuration(){return duration;}
}
