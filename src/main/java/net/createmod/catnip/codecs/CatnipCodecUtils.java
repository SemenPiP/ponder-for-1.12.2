package net.createmod.catnip.codecs;

import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.nbt.NBTBase;

public final class CatnipCodecUtils {
    private CatnipCodecUtils() {}
    public static <T> Optional<T> decode(Codec<T> codec, NBTBase tag) {
        try { return Optional.ofNullable(codec.decode(tag)); } catch (RuntimeException ignored) { return Optional.empty(); }
    }
    @Nullable public static <T> T decodeOrNull(Codec<T> codec, NBTBase tag) {
        try { return codec.decode(tag); } catch (RuntimeException ignored) { return null; }
    }
    public static <T> Optional<NBTBase> encode(Codec<T> codec, T value) {
        try { return Optional.ofNullable(codec.encode(value)); } catch (RuntimeException ignored) { return Optional.empty(); }
    }
}
