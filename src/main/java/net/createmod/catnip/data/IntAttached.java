package net.createmod.catnip.data;

import java.util.Comparator;
import java.util.function.Function;

import net.minecraft.nbt.NBTTagCompound;

public class IntAttached<V> extends Pair<Integer, V> {
    protected IntAttached(Integer number, V value) { super(number, value); }
    public static <V> IntAttached<V> with(int number, V value) { return new IntAttached<V>(number, value); }
    public static <V> IntAttached<V> withZero(V value) { return with(0, value); }
    public boolean isZero() { return first == 0; }
    public boolean exceeds(int value) { return first > value; }
    public boolean isOrBelowZero() { return first <= 0; }
    public void increment() { first++; }
    public void decrement() { first--; }
    public V getValue() { return second; }
    public NBTTagCompound serializeNBT(Function<V, NBTTagCompound> serializer) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("Item", serializer.apply(second));
        tag.setInteger("Location", first);
        return tag;
    }
    public static Comparator<IntAttached<?>> comparator() {
        return new Comparator<IntAttached<?>>() {
            public int compare(IntAttached<?> a, IntAttached<?> b) { return Integer.compare(b.first, a.first); }
        };
    }
    public static <T> IntAttached<T> read(NBTTagCompound tag, Function<NBTTagCompound, T> deserializer) {
        return with(tag.getInteger("Location"), deserializer.apply(tag.getCompoundTag("Item")));
    }
}
