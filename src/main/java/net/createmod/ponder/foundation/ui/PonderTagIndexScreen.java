package net.createmod.ponder.foundation.ui;

import java.util.List;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderTag;

public class PonderTagIndexScreen extends AbstractPonderScreen {
    @Override public void initGui(){buttonList.clear();List<PonderTag> tags=PonderIndex.getTagAccess().getListedTags();int columns=Math.max(1,Math.min(9,(width-40)/42));for(int i=0;i<tags.size();i++){final PonderTag tag=tags.get(i);int x=width/2-columns*42/2+(i%columns)*42;int y=60+(i/columns)*42;addButton(new PonderButton(100+i,x,y,32,32,null).showing(tag).withCallback(new Runnable(){@Override public void run(){ScreenOpener.open(new PonderTagScreen(tag));}}));}addButton(new PonderButton(1,8,height-29,22,20,PonderButton.Icon.INDEX).withCallback(new Runnable(){@Override public void run(){ScreenOpener.open(new PonderIndexScreen());}}));}
    @Override protected void renderWindow(int mouseX,int mouseY,float partialTicks){String title="Ponder Tags";fontRenderer.drawString(title,width/2-fontRenderer.getStringWidth(title)/2,14,0xffeef2f5);}
}
