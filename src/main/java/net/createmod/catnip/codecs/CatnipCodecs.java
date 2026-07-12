package net.createmod.catnip.codecs;

import java.util.List;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public final class CatnipCodecs {
    public static final Codec<Boolean> BOOL = Codecs.BOOL;
    public static final Codec<Integer> INT = Codecs.INT;
    public static final Codec<Long> LONG = Codecs.LONG;
    public static final Codec<Float> FLOAT = Codecs.FLOAT;
    public static final Codec<Double> DOUBLE = Codecs.DOUBLE;
    public static final Codec<Character> CHAR = Codecs.CHAR;
    public static final Codec<String> STRING = Codecs.STRING;
    public static final Codec<ResourceLocation> RESOURCE_LOCATION = Codecs.RESOURCE_LOCATION;
    public static final Codec<NBTTagCompound> COMPOUND = Codecs.COMPOUND;
    private CatnipCodecs() {}
    public static <E> Codec<Set<E>> set(Codec<E> codec) { return Codecs.set(codec); }
    public static <E> Codec<List<E>> list(Codec<E> codec) { return Codecs.list(codec); }
}
