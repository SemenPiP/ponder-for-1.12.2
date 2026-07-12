package net.createmod.catnip.codecs;

import java.util.function.Function;

import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

public interface Codec<T> {
    NBTBase encode(T value) throws CodecException;
    T decode(NBTBase value) throws CodecException;

    default <R> Codec<R> xmap(final Function<T, R> decoder, final Function<R, T> encoder) {
        final Codec<T> self = this;
        return new Codec<R>() {
            public NBTBase encode(R value) { return self.encode(encoder.apply(value)); }
            public R decode(NBTBase value) { return decoder.apply(self.decode(value)); }
        };
    }

    default String encodeJson(T value) {
        NBTBase encoded = encode(value);
        if (encoded instanceof NBTTagCompound) return encoded.toString();
        NBTTagCompound wrapper = new NBTTagCompound();
        wrapper.setTag("value", encoded);
        return wrapper.toString();
    }

    default T decodeJson(String json) {
        try {
            NBTTagCompound parsed = JsonToNBT.getTagFromJson(json);
            return decode(parsed.hasKey("value") && parsed.getKeySet().size() == 1 ? parsed.getTag("value") : parsed);
        } catch (Exception e) {
            throw new CodecException("Invalid JSON/SNBT input", e);
        }
    }
}
