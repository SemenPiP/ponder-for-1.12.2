package net.createmod.catnip.data;

import java.util.Objects;

import net.createmod.catnip.codecs.Codec;
import net.createmod.catnip.codecs.Codecs;
import net.createmod.catnip.codecs.stream.StreamCodec;
import io.netty.buffer.ByteBuf;

public class Pair<F, S> {
    protected F first;
    protected S second;
    protected Pair(F first, S second) { this.first = first; this.second = second; }
    public static <F, S> Pair<F, S> of(F first, S second) { return new Pair<F, S>(first, second); }
    public F getFirst() { return first; }
    public S getSecond() { return second; }
    public void setFirst(F first) { this.first = first; }
    public void setSecond(S second) { this.second = second; }
    public Pair<F, S> copy() { return of(first, second); }
    public Pair<S, F> swap() { return of(second, first); }
    public boolean equals(Object object) {
        if (object == this) return true;
        if (!(object instanceof Pair)) return false;
        Pair<?, ?> other = (Pair<?, ?>) object;
        return Objects.equals(first, other.first) && Objects.equals(second, other.second);
    }
    public int hashCode() { return Objects.hash(first, second); }
    public String toString() { return "(" + first + ", " + second + ")"; }

    public static <F, S> Codec<Pair<F, S>> codec(final Codec<F> firstCodec, final Codec<S> secondCodec) {
        return Codecs.compound(new Codecs.CompoundEncoder<Pair<F, S>>() {
            public void encode(Pair<F, S> value, net.minecraft.nbt.NBTTagCompound tag) {
                tag.setTag("first", firstCodec.encode(value.first));
                tag.setTag("second", secondCodec.encode(value.second));
            }
            public Pair<F, S> decode(net.minecraft.nbt.NBTTagCompound tag) {
                return Pair.of(firstCodec.decode(tag.getTag("first")), secondCodec.decode(tag.getTag("second")));
            }
        });
    }

    public static <B extends ByteBuf, F, S> StreamCodec<B, Pair<F, S>> streamCodec(
            final StreamCodec<? super B, F> firstCodec, final StreamCodec<? super B, S> secondCodec) {
        return new StreamCodec<B, Pair<F, S>>() {
            public Pair<F, S> decode(B buffer) { return Pair.of(firstCodec.decode(buffer), secondCodec.decode(buffer)); }
            public void encode(B buffer, Pair<F, S> value) {
                firstCodec.encode(buffer, value.first);
                secondCodec.encode(buffer, value.second);
            }
        };
    }
}
