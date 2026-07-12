package net.createmod.ponder.foundation.ui;

import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderTag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class PonderTagScreen extends AbstractPonderScreen {
    private final PonderTag tag;
    public PonderTagScreen(ResourceLocation id){this(PonderIndex.getTagAccess().getRegisteredTag(id));}
    public PonderTagScreen(PonderTag tag){this.tag=tag;}
    @Override public void initGui(){buttonList.clear();List<ResourceLocation> items=new ArrayList<ResourceLocation>(PonderIndex.getTagAccess().getItems(tag));int columns=Math.max(1,Math.min(9,(width-40)/36));for(int i=0;i<items.size();i++){final ResourceLocation component=items.get(i);ItemStack stack=PonderIndexScreen.stackFor(component);if(stack.isEmpty())continue;int x=width/2-columns*36/2+(i%columns)*36;int y=72+(i/columns)*36;addButton(new PonderButton(100+i,x,y,28,28,null).showing(stack).withCallback(new Runnable(){@Override public void run(){ScreenOpener.open(PonderUI.of(component));}}));}addButton(new PonderButton(1,8,height-29,22,20,PonderButton.Icon.TAGS).withCallback(new Runnable(){@Override public void run(){ScreenOpener.open(new PonderTagIndexScreen());}}));}
    @Override protected void renderWindow(int mouseX,int mouseY,float partialTicks){fontRenderer.drawString(tag.getTitle(),width/2-fontRenderer.getStringWidth(tag.getTitle())/2,12,0xffeef2f5);List<String> lines=fontRenderer.listFormattedStringToWidth(tag.getDescription(),Math.min(360,width-40));int y=30;for(String line:lines){fontRenderer.drawString(line,width/2-fontRenderer.getStringWidth(line)/2,y,0xffaeb8c2);y+=10;}}
}
