package net.createmod.catnip.nbt;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import javax.annotation.Nullable;

import net.createmod.catnip.components.ComponentProcessors;
import net.minecraft.block.BlockSign;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.text.ITextComponent;

public final class NBTProcessors {
    private static final Map<Class<? extends TileEntity>,UnaryOperator<NBTTagCompound>> PROCESSORS=new HashMap<Class<? extends TileEntity>,UnaryOperator<NBTTagCompound>>();
    private static final Map<Class<? extends TileEntity>,UnaryOperator<NBTTagCompound>> SURVIVAL_PROCESSORS=new HashMap<Class<? extends TileEntity>,UnaryOperator<NBTTagCompound>>();
    private NBTProcessors(){}
    public static synchronized void addProcessor(Class<? extends TileEntity> type,UnaryOperator<NBTTagCompound> processor){PROCESSORS.put(type,processor);}
    public static synchronized void addSurvivalProcessor(Class<? extends TileEntity> type,UnaryOperator<NBTTagCompound> processor){SURVIVAL_PROCESSORS.put(type,processor);}
    public static UnaryOperator<NBTTagCompound> itemProcessor(final String tagKey){return new UnaryOperator<NBTTagCompound>(){public NBTTagCompound apply(NBTTagCompound data){if(!data.hasKey(tagKey,10))return data;NBTTagCompound item=data.getCompoundTag(tagKey);if(item.hasKey("tag",10)){NBTTagCompound clean=ComponentProcessors.sanitizeItemTag(item.getCompoundTag("tag"));if(clean==null)item.removeTag("tag");else item.setTag("tag",clean);}return data;}};}
    public static boolean textComponentHasClickEvent(ITextComponent component){return ComponentProcessors.textComponentHasClickEvent(component);}
    @Nullable public static NBTTagCompound process(IBlockState state,TileEntity tile,@Nullable NBTTagCompound tag,boolean survival){
        if(tag==null)return null;
        UnaryOperator<NBTTagCompound> processor=find(survival?SURVIVAL_PROCESSORS:null,tile.getClass());if(processor!=null)tag=processor.apply(tag);
        if(tag==null)return null;processor=find(PROCESSORS,tile.getClass());if(processor!=null)return processor.apply(tag);
        if(tile instanceof TileEntityMobSpawner)return tag;
        if(tile instanceof TileEntitySign||state.getBlock() instanceof BlockSign){if(tile instanceof TileEntitySign)for(ITextComponent line:((TileEntitySign)tile).signText)if(line!=null&&textComponentHasClickEvent(line))return null;}
        if(tile.onlyOpsCanSetNbt())return null;
        return tag;
    }
    @Nullable private static UnaryOperator<NBTTagCompound> find(Map<Class<? extends TileEntity>,UnaryOperator<NBTTagCompound>> map,Class<?> type){if(map==null)return null;UnaryOperator<NBTTagCompound> exact=map.get(type);if(exact!=null)return exact;for(Map.Entry<Class<? extends TileEntity>,UnaryOperator<NBTTagCompound>> entry:map.entrySet())if(entry.getKey().isAssignableFrom(type))return entry.getValue();return null;}
}
