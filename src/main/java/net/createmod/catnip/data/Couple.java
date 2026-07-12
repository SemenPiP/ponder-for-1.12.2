package net.createmod.catnip.data;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import net.createmod.catnip.codecs.Codec;
import net.createmod.catnip.codecs.stream.StreamCodec;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import io.netty.buffer.ByteBuf;

public class Couple<T> extends Pair<T, T> implements Iterable<T> {
    private static final Couple<Boolean> TRUE_AND_FALSE = Couple.create(true, false);
    protected Couple(T first, T second) { super(first, second); }
    public static <T> Couple<T> create(T first, T second) { return new Couple<T>(first, second); }
    public static <T> Couple<T> create(Supplier<T> factory) { return create(factory.get(), factory.get()); }
    public static <T> Couple<T> createWithContext(Function<Boolean, T> factory) {
        return create(factory.apply(true), factory.apply(false));
    }
    public T get(boolean first) { return first ? this.first : second; }
    public void set(boolean first, T value) { if (first) this.first = value; else second = value; }
    public Couple<T> copy() { return create(first, second); }
    public Couple<T> swap() { return create(second, first); }
    public <S> Couple<S> map(Function<T, S> function) { return create(function.apply(first), function.apply(second)); }
    public <S> Couple<S> mapNotNull(Function<T, S> function) {
        return create(first == null ? null : function.apply(first), second == null ? null : function.apply(second));
    }
    public <S> Couple<S> mapWithContext(BiFunction<T, Boolean, S> function) {
        return create(function.apply(first, true), function.apply(second, false));
    }
    public <S, R> Couple<S> mapWithParams(BiFunction<T, R, S> function, Couple<R> values) {
        return create(function.apply(first, values.first), function.apply(second, values.second));
    }
    public <S, R> Couple<S> mapNotNullWithParam(BiFunction<T, R, S> function, R value) {
        return create(first == null ? null : function.apply(first, value), second == null ? null : function.apply(second, value));
    }
    public boolean both(Predicate<T> test) { return test.test(first) && test.test(second); }
    public boolean either(Predicate<T> test) { return test.test(first) || test.test(second); }
    public void replace(Function<T, T> function) { first = function.apply(first); second = function.apply(second); }
    public void replaceWithContext(BiFunction<T, Boolean, T> function) { replaceWithParams(function, TRUE_AND_FALSE); }
    public <S> void replaceWithParams(BiFunction<T, S, T> function, Couple<S> values) {
        first = function.apply(first, values.first); second = function.apply(second, values.second);
    }
    public void forEach(Consumer<? super T> consumer) { consumer.accept(first); consumer.accept(second); }
    public void forEachWithContext(BiConsumer<T, Boolean> consumer) { forEachWithParams(consumer, TRUE_AND_FALSE); }
    public <S> void forEachWithParams(BiConsumer<T, S> consumer, Couple<S> values) {
        consumer.accept(first, values.first); consumer.accept(second, values.second);
    }
    public NBTTagList serializeEach(Function<T, NBTTagCompound> serializer) {
        return NBTHelper.writeCompoundList(Arrays.asList(first, second), serializer);
    }
    public static <S> Couple<S> deserializeEach(NBTTagList list, Function<NBTTagCompound, S> deserializer) {
        if (list.tagCount() != 2) throw new IllegalArgumentException("A serialized Couple must have exactly two entries");
        List<S> values = NBTHelper.readCompoundList(list, deserializer);
        return create(values.get(0), values.get(1));
    }
    public static <T> Codec<Couple<T>> codec(final Codec<T> codec) {
        final Codec<Pair<T, T>> pairCodec = Pair.codec(codec, codec);
        return pairCodec.xmap(new Function<Pair<T, T>, Couple<T>>() {
            public Couple<T> apply(Pair<T, T> pair) { return Couple.create(pair.first, pair.second); }
        }, new Function<Couple<T>, Pair<T, T>>() {
            public Pair<T, T> apply(Couple<T> couple) { return couple; }
        });
    }
    public static <B extends ByteBuf, T> StreamCodec<B, Couple<T>> streamCodec(final StreamCodec<? super B, T> codec) {
        return new StreamCodec<B, Couple<T>>() {
            public Couple<T> decode(B buffer) { return create(codec.decode(buffer), codec.decode(buffer)); }
            public void encode(B buffer, Couple<T> value) { codec.encode(buffer, value.first); codec.encode(buffer, value.second); }
        };
    }
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private int index;
            public boolean hasNext() { return index < 2; }
            public T next() {
                if (!hasNext()) throw new NoSuchElementException();
                return index++ == 0 ? first : second;
            }
            public void remove() { throw new UnsupportedOperationException(); }
        };
    }
    public Stream<T> stream() { return Stream.of(first, second); }
}
