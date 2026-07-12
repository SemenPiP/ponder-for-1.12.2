package net.createmod.catnip.components;

import static org.junit.Assert.*;

import org.junit.Test;

import net.minecraft.nbt.NBTTagCompound;

public class ComponentProcessorsTest {
    @Test public void sanitizerKeepsGameplayDataAndDropsArbitraryTags(){
        NBTTagCompound source=new NBTTagCompound();source.setString("Potion","minecraft:water");source.setString("ArbitraryModPayload","unsafe");
        NBTTagCompound clean=ComponentProcessors.sanitizeItemTag(source);assertNotNull(clean);assertEquals("minecraft:water",clean.getString("Potion"));assertFalse(clean.hasKey("ArbitraryModPayload"));
    }
    @Test public void sanitizerDropsClickableCustomName(){
        NBTTagCompound source=new NBTTagCompound(),display=new NBTTagCompound();
        display.setString("Name","{\"text\":\"click\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/op x\"}}");source.setTag("display",display);
        assertNull(ComponentProcessors.sanitizeItemTag(source));
    }
}
