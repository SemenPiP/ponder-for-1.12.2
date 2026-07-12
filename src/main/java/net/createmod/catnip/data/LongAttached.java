package net.createmod.catnip.data;

import java.util.Comparator;
import java.util.function.Function;

import net.minecraft.nbt.NBTTagCompound;

public class LongAttached<V> extends Pair<Long, V> {
    protected LongAttached(Long number, V value) { super(number, value); }
    public static <V> LongAttached<V> with(long number, V value) { return new LongAttached<V>(number, value); }
    public static <V> LongAttached<V> withZero(V value) { return with(0, value); }
    public boolean isZero() { return first == 0; }
    public boolean exceeds(long value) { return first > value; }
    public boolean isOrBelowZero() { return first <= 0; }
    public void increment() { first++; }
    public void decrement() { first--; }
    public V getValue() { return second; }
    public NBTTagCompound serializeNBT(Function<V, NBTTagCompound> serializer) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("Item", serializer.apply(second));
        tag.setLong("Location", first);
        return tag;
    }
    public static Comparator<LongAttached<?>> comparator() {
        return new Comparator<LongAttached<?>>() {
            public int compare(LongAttached<?> a, LongAttached<?> b) { return Long.compare(b.first, a.first); }
        };
    }
    public static <T> LongAttached<T> read(NBTTagCompound tag, Function<NBTTagCompound, T> deserializer) {
        return with(tag.getLong("Location"), deserializer.apply(tag.getCompoundTag("Item")));
    }
}
