package net.createmod.ponder.foundation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

public final class PonderTooltipHandler {
    private static final List<Consumer<ItemStack>> CALLBACKS=new ArrayList<Consumer<ItemStack>>();private static ItemStack tracked=ItemStack.EMPTY;private static long lastSeen;private static float progress;
    private PonderTooltipHandler(){}
    public static void onTooltip(ItemTooltipEvent event){if(event.getEntityPlayer()==null||event.getItemStack().isEmpty())return;ResourceLocation id=Item.REGISTRY.getNameForObject(event.getItemStack().getItem());if(id==null||!PonderIndex.getSceneAccess().doScenesExistForId(id))return;if(tracked.isEmpty()||tracked.getItem()!=event.getItemStack().getItem()){tracked=event.getItemStack().copy();progress=0;}lastSeen=System.currentTimeMillis();for(Consumer<ItemStack> callback:CALLBACKS)callback.accept(tracked.copy());int filled=Math.round(progress*10);StringBuilder bar=new StringBuilder(TextFormatting.DARK_GRAY.toString()).append('[');for(int i=0;i<10;i++)bar.append(i<filled?TextFormatting.AQUA.toString()+"|":TextFormatting.DARK_GRAY.toString()+"|");bar.append(TextFormatting.DARK_GRAY).append(']');event.getToolTip().add(bar.toString());}
    public static void tick(){Minecraft minecraft=Minecraft.getMinecraft();boolean fresh=!tracked.isEmpty()&&System.currentTimeMillis()-lastSeen<180;boolean held=fresh&&minecraft.currentScreen!=null&&minecraft.gameSettings.keyBindForward.isKeyDown();progress=Math.max(0,Math.min(1,progress+(held?.09f:-.12f)));if(progress>=1){ItemStack opening=tracked.copy();progress=0;tracked=ItemStack.EMPTY;try{ScreenOpener.open(PonderUI.of(opening));}catch(RuntimeException ignored){}}if(!fresh&&progress==0)tracked=ItemStack.EMPTY;}
    public static float getProgress(){return progress;}public static synchronized void registerHoveredPonderStackCallback(Consumer<ItemStack> callback){if(callback!=null)CALLBACKS.add(callback);}public static synchronized void removeHoveredPonderStackCallback(Consumer<ItemStack> callback){CALLBACKS.remove(callback);}
}
