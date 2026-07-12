package net.createmod.catnip.codecs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTPrimitive;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;

public final class Codecs {
    private Codecs() {}

    public static final Codec<Boolean> BOOL = primitive(
        new Function<Boolean, NBTBase>() { public NBTBase apply(Boolean v) { return new NBTTagByte((byte) (v ? 1 : 0)); } },
        new Function<NBTBase, Boolean>() { public Boolean apply(NBTBase v) { return number(v).getByte() != 0; } });
    public static final Codec<Integer> INT = primitive(
        new Function<Integer, NBTBase>() { public NBTBase apply(Integer v) { return new NBTTagInt(v); } },
        new Function<NBTBase, Integer>() { public Integer apply(NBTBase v) { return number(v).getInt(); } });
    public static final Codec<Long> LONG = primitive(
        new Function<Long, NBTBase>() { public NBTBase apply(Long v) { return new NBTTagLong(v); } },
        new Function<NBTBase, Long>() { public Long apply(NBTBase v) { return number(v).getLong(); } });
    public static final Codec<Float> FLOAT = primitive(
        new Function<Float, NBTBase>() { public NBTBase apply(Float v) { return new NBTTagFloat(v); } },
        new Function<NBTBase, Float>() { public Float apply(NBTBase v) { return number(v).getFloat(); } });
    public static final Codec<Double> DOUBLE = primitive(
        new Function<Double, NBTBase>() { public NBTBase apply(Double v) { return new NBTTagDouble(v); } },
        new Function<NBTBase, Double>() { public Double apply(NBTBase v) { return number(v).getDouble(); } });
    public static final Codec<Character> CHAR = INT.xmap(
        new Function<Integer, Character>() { public Character apply(Integer v) {
            if (v < Character.MIN_VALUE || v > Character.MAX_VALUE) throw new CodecException("Character out of range: " + v);
            return (char) v.intValue();
        } },
        new Function<Character, Integer>() { public Integer apply(Character v) { return (int) v.charValue(); } });
    public static final Codec<String> STRING = new Codec<String>() {
        public NBTBase encode(String value) { return new NBTTagString(value); }
        public String decode(NBTBase value) {
            if (!(value instanceof NBTTagString)) throw typeError("string", value);
            return ((NBTTagString) value).getString();
        }
    };
    public static final Codec<ResourceLocation> RESOURCE_LOCATION = STRING.xmap(
        new Function<String, ResourceLocation>() { public ResourceLocation apply(String id) {
            try { return new ResourceLocation(id); } catch (RuntimeException e) { throw new CodecException("Invalid resource location: " + id, e); }
        } },
        new Function<ResourceLocation, String>() { public String apply(ResourceLocation id) { return id.toString(); } });
    public static final Codec<NBTTagCompound> COMPOUND = new Codec<NBTTagCompound>() {
        public NBTBase encode(NBTTagCompound value) { return value.copy(); }
        public NBTTagCompound decode(NBTBase value) {
            if (!(value instanceof NBTTagCompound)) throw typeError("compound", value);
            return ((NBTTagCompound) value).copy();
        }
    };

    public static <T> Codec<T> primitive(final Function<T, NBTBase> encoder, final Function<NBTBase, T> decoder) {
        return new Codec<T>() {
            public NBTBase encode(T value) { if (value == null) throw new CodecException("Cannot encode null"); return encoder.apply(value); }
            public T decode(NBTBase value) { if (value == null) throw new CodecException("Cannot decode null"); return decoder.apply(value); }
        };
    }

    public static <T> Codec<T> compound(final CompoundEncoder<T> adapter) {
        return new Codec<T>() {
            public NBTBase encode(T value) {
                if (value == null) throw new CodecException("Cannot encode null");
                NBTTagCompound tag = new NBTTagCompound(); adapter.encode(value, tag); return tag;
            }
            public T decode(NBTBase value) {
                if (!(value instanceof NBTTagCompound)) throw typeError("compound", value);
                return adapter.decode((NBTTagCompound) value);
            }
        };
    }

    public static <T> Codec<List<T>> list(final Codec<T> elementCodec) { return list(elementCodec, 65535); }
    public static <T> Codec<List<T>> list(final Codec<T> elementCodec, final int maxSize) {
        if (maxSize < 0) throw new IllegalArgumentException("maxSize cannot be negative");
        return new Codec<List<T>>() {
            public NBTBase encode(List<T> values) {
                if (values.size() > maxSize) throw new CodecException("List exceeds maximum size of " + maxSize);
                NBTTagList list = new NBTTagList();
                for (T value : values) list.appendTag(elementCodec.encode(value));
                return list;
            }
            public List<T> decode(NBTBase value) {
                if (!(value instanceof NBTTagList)) throw typeError("list", value);
                NBTTagList input = (NBTTagList) value;
                if (input.tagCount() > maxSize) throw new CodecException("List exceeds maximum size of " + maxSize);
                List<T> result = new ArrayList<T>(input.tagCount());
                for (int i = 0; i < input.tagCount(); i++) result.add(elementCodec.decode(input.get(i)));
                return result;
            }
        };
    }

    public static <T> Codec<Set<T>> set(final Codec<T> elementCodec) {
        return list(elementCodec).xmap(
            new Function<List<T>, Set<T>>() { public Set<T> apply(List<T> v) { return new HashSet<T>(v); } },
            new Function<Set<T>, List<T>>() { public List<T> apply(Set<T> v) { return new ArrayList<T>(v); } });
    }

    public static <T> Codec<Optional<T>> optional(final Codec<T> base) {
        return compound(new CompoundEncoder<Optional<T>>() {
            public void encode(Optional<T> value, NBTTagCompound tag) { if (value.isPresent()) tag.setTag("value", base.encode(value.get())); }
            public Optional<T> decode(NBTTagCompound tag) { return tag.hasKey("value") ? Optional.of(base.decode(tag.getTag("value"))) : Optional.<T>empty(); }
        });
    }

    public static <E extends Enum<E>> Codec<E> enumCodec(final Class<E> type) {
        final E[] values = type.getEnumConstants();
        if (values == null) throw new IllegalArgumentException(type + " is not an enum");
        return STRING.xmap(new Function<String, E>() {
            public E apply(String name) {
                for (E value : values) if (value.name().equalsIgnoreCase(name)) return value;
                throw new CodecException("Unknown " + type.getSimpleName() + " value: " + name);
            }
        }, new Function<E, String>() { public String apply(E value) { return value.name(); } });
    }

    private static NBTPrimitive number(NBTBase value) {
        if (!(value instanceof NBTPrimitive)) throw typeError("number", value);
        return (NBTPrimitive) value;
    }
    private static CodecException typeError(String expected, NBTBase actual) {
        return new CodecException("Expected NBT " + expected + ", got " + (actual == null ? "null" : actual.getClass().getSimpleName()));
    }

    public interface CompoundEncoder<T> {
        void encode(T value, NBTTagCompound tag);
        T decode(NBTTagCompound tag);
    }
}
