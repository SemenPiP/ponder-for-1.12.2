package net.createmod.catnip.components;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.ITextComponent;

public final class ComponentProcessors {
    private static final Set<String> SAFE_TAGS = new HashSet<String>(Arrays.asList("ench","StoredEnchantments","Potion","CustomPotionEffects","RepairCost","display"));
    private ComponentProcessors() {}
    public static ItemStack withUnsafeComponentsDiscarded(ItemStack stack){
        if(stack.isEmpty()||!stack.hasTagCompound())return stack;
        ItemStack copy=stack.copy();copy.setTagCompound(sanitizeItemTag(copy.getTagCompound()));return copy;
    }
    public static NBTTagCompound sanitizeItemTag(NBTTagCompound source){
        NBTTagCompound clean=new NBTTagCompound();
        for(String key:SAFE_TAGS)if(source.hasKey(key))clean.setTag(key,source.getTag(key).copy());
        if(clean.hasKey("display",10)){
            NBTTagCompound display=clean.getCompoundTag("display");
            for(String key:new java.util.HashSet<String>(display.getKeySet()))if(!"Name".equals(key)&&!"color".equals(key))display.removeTag(key);
            if(display.hasKey("Name",8)&&hasUnsafeText(display.getString("Name")))display.removeTag("Name");
            if(display.isEmpty())clean.removeTag("display");
        }
        return clean.isEmpty()?null:clean;
    }
    private static boolean hasUnsafeText(String json){
        try{ITextComponent component=ITextComponent.Serializer.jsonToComponent(json);return component!=null&&textComponentHasClickEvent(component);}catch(RuntimeException e){return true;}
    }
    public static boolean textComponentHasClickEvent(ITextComponent component){
        if(component.getStyle()!=null&&component.getStyle().getClickEvent()!=null)return true;
        for(ITextComponent sibling:component.getSiblings())if(textComponentHasClickEvent(sibling))return true;
        return false;
    }
}
