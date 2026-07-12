package net.createmod.catnip.registry;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

public final class RegisteredObjectsHelper {
    private RegisteredObjectsHelper() {}
    public static <V extends IForgeRegistryEntry<V>> ResourceLocation getKeyOrThrow(IForgeRegistry<V> registry,V value){ResourceLocation key=registry.getKey(value);if(key==null)throw new IllegalArgumentException("Unregistered value: "+value);return key;}
    public static ResourceLocation getKeyOrThrow(Block value){return getKeyOrThrow(ForgeRegistries.BLOCKS,value);}
    public static ResourceLocation getKeyOrThrow(Item value){return getKeyOrThrow(ForgeRegistries.ITEMS,value);}
    public static ResourceLocation getKeyOrThrow(Potion value){return getKeyOrThrow(ForgeRegistries.POTIONS,value);}
    public static ResourceLocation getKeyOrThrow(Fluid value){String key=FluidRegistry.getDefaultFluidName(value);if(key==null)throw new IllegalArgumentException("Unregistered fluid: "+value);return new ResourceLocation(key);}
    public static ResourceLocation getKeyOrThrow(Class<? extends Entity> value){EntityRegistry.EntityRegistration registration=EntityRegistry.instance().lookupModSpawn(value,false);if(registration==null)throw new IllegalArgumentException("Unregistered entity class: "+value);return registration.getRegistryName();}
    @Nullable public static Item getItem(ResourceLocation location){return ForgeRegistries.ITEMS.getValue(location);}
    @Nullable public static Block getBlock(ResourceLocation location){return ForgeRegistries.BLOCKS.getValue(location);}
    @Nullable public static Object getItemOrBlock(ResourceLocation location){Item item=getItem(location);return item!=null?item:getBlock(location);}
    public static ResourceLocation getKeyOrThrow(Object itemLike){if(itemLike instanceof Item)return getKeyOrThrow((Item)itemLike);if(itemLike instanceof Block)return getKeyOrThrow((Block)itemLike);throw new IllegalArgumentException("Expected Item or Block, got "+itemLike);}
}
