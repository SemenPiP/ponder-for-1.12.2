package net.createmod.ponder.foundation.ui;

import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

public class PonderProgressBar extends AbstractSimiWidget {
    private final PonderUI ponder;
    public PonderProgressBar(int id,PonderUI ponder,int x,int y,int width,int height){super(id,x,y,width,height,"");this.ponder=ponder;}
    @Override public void drawButton(Minecraft mc,int mouseX,int mouseY,float partialTicks){
        if(!visible)return;PonderScene scene=ponder.getActiveScene();hovered=mouseX>=x&&mouseY>=y-4&&mouseX<x+width&&mouseY<y+height+8;
        Gui.drawRect(x,y,x+width,y+height,0xff303740);int progress=Math.round(width*scene.getSceneProgress());Gui.drawRect(x,y,x+progress,y+height,0xff94aeca);
        int total=Math.max(1,scene.getTotalTicks());for(Integer tick:scene.getKeyframes()){int marker=x+Math.round(width*tick/(float)total);Gui.drawRect(marker-1,y-3,marker+1,y+height+3,0xffd8e1ea);}
    }
    @Override public boolean mousePressed(Minecraft mc,int mouseX,int mouseY){
        if(!super.mousePressed(mc,mouseX,mouseY))return false;seek(mouseX);return true;
    }
    public void dragTo(int mouseX){seek(mouseX);}
    private void seek(int mouseX){PonderScene scene=ponder.getActiveScene();float value=Math.max(0,Math.min(1,(mouseX-x)/(float)Math.max(1,width)));scene.seek(Math.round(scene.getTotalTicks()*value));}
}
