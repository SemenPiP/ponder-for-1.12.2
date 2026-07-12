package net.createmod.ponder.foundation.ui;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.ponder.api.registration.StoryBoardEntry;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class PonderIndexScreen extends AbstractPonderScreen {
    private final Set<ResourceLocation> components=new LinkedHashSet<ResourceLocation>();
    public PonderIndexScreen(){for(Map.Entry<ResourceLocation,StoryBoardEntry> entry:PonderIndex.getSceneAccess().getRegisteredEntries())components.add(entry.getKey());}
    @Override public void initGui(){buttonList.clear();int columns=Math.max(1,Math.min(9,(width-40)/36));int i=0;for(final ResourceLocation component:components){ItemStack stack=stackFor(component);if(stack.isEmpty())continue;int x=width/2-columns*36/2+(i%columns)*36;int y=60+(i/columns)*36;addButton(new PonderButton(100+i,x,y,28,28,null).showing(stack).withCallback(new Runnable(){@Override public void run(){ScreenOpener.open(PonderUI.of(component));}}));i++;}addButton(new PonderButton(1,8,height-29,22,20,PonderButton.Icon.TAGS).withCallback(new Runnable(){@Override public void run(){ScreenOpener.open(new PonderTagIndexScreen());}}));addButton(new PonderButton(2,width-30,height-29,22,20,PonderButton.Icon.CLOSE).withCallback(new Runnable(){@Override public void run(){mc.displayGuiScreen(null);}}));}
    @Override protected void renderWindow(int mouseX,int mouseY,float partialTicks){fontRenderer.drawString("Ponder",width/2-fontRenderer.getStringWidth("Ponder")/2,14,0xffeef2f5);}
    static ItemStack stackFor(ResourceLocation id){Item item=Item.REGISTRY.getObject(id);if(item!=null)return new ItemStack(item);Block block=Block.REGISTRY.getObject(id);return block==null?ItemStack.EMPTY:new ItemStack(block);}
}
