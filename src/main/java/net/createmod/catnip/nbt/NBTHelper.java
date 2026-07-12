package net.createmod.catnip.nbt;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

public final class NBTHelper {
    private NBTHelper() {}
    public static void putMarker(NBTTagCompound nbt,String marker){nbt.setBoolean(marker,true);}
    public static NBTTagCompound writeBlockPos(BlockPos pos){NBTTagCompound tag=new NBTTagCompound();tag.setInteger("X",pos.getX());tag.setInteger("Y",pos.getY());tag.setInteger("Z",pos.getZ());return tag;}
    public static BlockPos readBlockPos(NBTTagCompound nbt,String key){NBTTagCompound tag=nbt.getCompoundTag(key);return new BlockPos(tag.getInteger("X"),tag.getInteger("Y"),tag.getInteger("Z"));}
    public static <T extends Enum<?>> T readEnum(NBTTagCompound nbt,String key,Class<T> type){T[] values=type.getEnumConstants();if(values==null||values.length==0)throw new IllegalArgumentException("Not an enum: "+type);String name=nbt.getString(key);for(T value:values)if(value.name().equalsIgnoreCase(name))return value;return values[0];}
    public static <T extends Enum<?>> void writeEnum(NBTTagCompound nbt,String key,T value){nbt.setString(key,value.name());}
    public static <T> NBTTagList writeCompoundList(Iterable<T> values,Function<T,NBTTagCompound> serializer){NBTTagList list=new NBTTagList();for(T value:values){NBTTagCompound tag=serializer.apply(value);if(tag!=null)list.appendTag(tag);}return list;}
    public static <T> List<T> readCompoundList(NBTTagList list,Function<NBTTagCompound,T> deserializer){List<T> result=new ArrayList<T>(list.tagCount());for(int i=0;i<list.tagCount();i++)result.add(deserializer.apply(list.getCompoundTagAt(i)));return result;}
    public static void iterateCompoundList(NBTTagList list,Consumer<NBTTagCompound> consumer){for(int i=0;i<list.tagCount();i++)consumer.accept(list.getCompoundTagAt(i));}
    public static NBTTagList writeItemList(Iterable<ItemStack> stacks){NBTTagList list=new NBTTagList();for(ItemStack stack:stacks)list.appendTag(stack.writeToNBT(new NBTTagCompound()));return list;}
    public static List<ItemStack> readItemList(NBTTagList list){List<ItemStack> result=new ArrayList<ItemStack>(list.tagCount());for(int i=0;i<list.tagCount();i++)result.add(new ItemStack(list.getCompoundTagAt(i)));return result;}
    public static NBTTagList writeAABB(AxisAlignedBB box){NBTTagList list=new NBTTagList();list.appendTag(new NBTTagFloat((float)box.minX));list.appendTag(new NBTTagFloat((float)box.minY));list.appendTag(new NBTTagFloat((float)box.minZ));list.appendTag(new NBTTagFloat((float)box.maxX));list.appendTag(new NBTTagFloat((float)box.maxY));list.appendTag(new NBTTagFloat((float)box.maxZ));return list;}
    @Nullable public static AxisAlignedBB readAABB(NBTTagList list){return list.tagCount()<6?null:new AxisAlignedBB(list.getFloatAt(0),list.getFloatAt(1),list.getFloatAt(2),list.getFloatAt(3),list.getFloatAt(4),list.getFloatAt(5));}
    public static NBTTagList writeVec3i(Vec3i vec){NBTTagList list=new NBTTagList();list.appendTag(new NBTTagInt(vec.getX()));list.appendTag(new NBTTagInt(vec.getY()));list.appendTag(new NBTTagInt(vec.getZ()));return list;}
    public static Vec3i readVec3i(NBTTagList list){if(list.tagCount()<3)return new Vec3i(0,0,0);return new Vec3i(list.getIntAt(0),list.getIntAt(1),list.getIntAt(2));}
    public static NBTBase getINBT(NBTTagCompound nbt,String id){NBTBase value=nbt.getTag(id);return value==null?new NBTTagCompound():value;}
    public static NBTTagCompound intToCompound(int value){NBTTagCompound tag=new NBTTagCompound();tag.setInteger("V",value);return tag;}
    public static int intFromCompound(NBTTagCompound tag){return tag.getInteger("V");}
    public static void writeResourceLocation(NBTTagCompound nbt,String key,ResourceLocation location){nbt.setString(key,location.toString());}
    public static ResourceLocation readResourceLocation(NBTTagCompound nbt,String key){return new ResourceLocation(nbt.getString(key));}
}
