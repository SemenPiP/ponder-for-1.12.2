package net.createmod.catnip.codecs.stream;

import java.util.function.Function;

/** Java 8 composite stream codecs for records with seven through sixteen fields. */
public final class CatnipLargerStreamCodecs {
    private CatnipLargerStreamCodecs() {
    }

    public static <B,C,T1,T2,T3,T4,T5,T6,T7> StreamCodec<B,C> composite(
        StreamCodec<? super B,T1> c1, Function<C,T1> g1,
        StreamCodec<? super B,T2> c2, Function<C,T2> g2,
        StreamCodec<? super B,T3> c3, Function<C,T3> g3,
        StreamCodec<? super B,T4> c4, Function<C,T4> g4,
        StreamCodec<? super B,T5> c5, Function<C,T5> g5,
        StreamCodec<? super B,T6> c6, Function<C,T6> g6,
        StreamCodec<? super B,T7> c7, Function<C,T7> g7,
        final Function7<T1,T2,T3,T4,T5,T6,T7,C> factory) {
        return compose(codecs(c1,c2,c3,c4,c5,c6,c7), getters(g1,g2,g3,g4,g5,g6,g7),
            new ArrayFactory<C>() { @SuppressWarnings("unchecked") public C create(Object[] v) {
                return factory.apply((T1)v[0],(T2)v[1],(T3)v[2],(T4)v[3],(T5)v[4],(T6)v[5],(T7)v[6]); }});
    }

    public static <B,C,T1,T2,T3,T4,T5,T6,T7,T8> StreamCodec<B,C> composite(
        StreamCodec<? super B,T1> c1, Function<C,T1> g1,
        StreamCodec<? super B,T2> c2, Function<C,T2> g2,
        StreamCodec<? super B,T3> c3, Function<C,T3> g3,
        StreamCodec<? super B,T4> c4, Function<C,T4> g4,
        StreamCodec<? super B,T5> c5, Function<C,T5> g5,
        StreamCodec<? super B,T6> c6, Function<C,T6> g6,
        StreamCodec<? super B,T7> c7, Function<C,T7> g7,
        StreamCodec<? super B,T8> c8, Function<C,T8> g8,
        final Function8<T1,T2,T3,T4,T5,T6,T7,T8,C> factory) {
        return compose(codecs(c1,c2,c3,c4,c5,c6,c7,c8), getters(g1,g2,g3,g4,g5,g6,g7,g8),
            new ArrayFactory<C>() { @SuppressWarnings("unchecked") public C create(Object[] v) {
                return factory.apply((T1)v[0],(T2)v[1],(T3)v[2],(T4)v[3],(T5)v[4],(T6)v[5],(T7)v[6],(T8)v[7]); }});
    }

    public static <B,C,T1,T2,T3,T4,T5,T6,T7,T8,T9> StreamCodec<B,C> composite(
        StreamCodec<? super B,T1> c1, Function<C,T1> g1,
        StreamCodec<? super B,T2> c2, Function<C,T2> g2,
        StreamCodec<? super B,T3> c3, Function<C,T3> g3,
        StreamCodec<? super B,T4> c4, Function<C,T4> g4,
        StreamCodec<? super B,T5> c5, Function<C,T5> g5,
        StreamCodec<? super B,T6> c6, Function<C,T6> g6,
        StreamCodec<? super B,T7> c7, Function<C,T7> g7,
        StreamCodec<? super B,T8> c8, Function<C,T8> g8,
        StreamCodec<? super B,T9> c9, Function<C,T9> g9,
        final Function9<T1,T2,T3,T4,T5,T6,T7,T8,T9,C> factory) {
        return compose(codecs(c1,c2,c3,c4,c5,c6,c7,c8,c9), getters(g1,g2,g3,g4,g5,g6,g7,g8,g9),
            new ArrayFactory<C>() { @SuppressWarnings("unchecked") public C create(Object[] v) {
                return factory.apply((T1)v[0],(T2)v[1],(T3)v[2],(T4)v[3],(T5)v[4],(T6)v[5],(T7)v[6],(T8)v[7],(T9)v[8]); }});
    }

    public static <B,C,T1,T2,T3,T4,T5,T6,T7,T8,T9,T10> StreamCodec<B,C> composite(
        StreamCodec<? super B,T1> c1, Function<C,T1> g1,
        StreamCodec<? super B,T2> c2, Function<C,T2> g2,
        StreamCodec<? super B,T3> c3, Function<C,T3> g3,
        StreamCodec<? super B,T4> c4, Function<C,T4> g4,
        StreamCodec<? super B,T5> c5, Function<C,T5> g5,
        StreamCodec<? super B,T6> c6, Function<C,T6> g6,
        StreamCodec<? super B,T7> c7, Function<C,T7> g7,
        StreamCodec<? super B,T8> c8, Function<C,T8> g8,
        StreamCodec<? super B,T9> c9, Function<C,T9> g9,
        StreamCodec<? super B,T10> c10, Function<C,T10> g10,
        final Function10<T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,C> factory) {
        return compose(codecs(c1,c2,c3,c4,c5,c6,c7,c8,c9,c10), getters(g1,g2,g3,g4,g5,g6,g7,g8,g9,g10),
            new ArrayFactory<C>() { @SuppressWarnings("unchecked") public C create(Object[] v) {
                return factory.apply((T1)v[0],(T2)v[1],(T3)v[2],(T4)v[3],(T5)v[4],(T6)v[5],(T7)v[6],(T8)v[7],(T9)v[8],(T10)v[9]); }});
    }

    public static <B,C,T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,T11> StreamCodec<B,C> composite(
        StreamCodec<? super B,T1> c1, Function<C,T1> g1,
        StreamCodec<? super B,T2> c2, Function<C,T2> g2,
        StreamCodec<? super B,T3> c3, Function<C,T3> g3,
        StreamCodec<? super B,T4> c4, Function<C,T4> g4,
        StreamCodec<? super B,T5> c5, Function<C,T5> g5,
        StreamCodec<? super B,T6> c6, Function<C,T6> g6,
        StreamCodec<? super B,T7> c7, Function<C,T7> g7,
        StreamCodec<? super B,T8> c8, Function<C,T8> g8,
        StreamCodec<? super B,T9> c9, Function<C,T9> g9,
        StreamCodec<? super B,T10> c10, Function<C,T10> g10,
        StreamCodec<? super B,T11> c11, Function<C,T11> g11,
        final Function11<T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,T11,C> factory) {
        return compose(codecs(c1,c2,c3,c4,c5,c6,c7,c8,c9,c10,c11), getters(g1,g2,g3,g4,g5,g6,g7,g8,g9,g10,g11),
            new ArrayFactory<C>() { @SuppressWarnings("unchecked") public C create(Object[] v) {
                return factory.apply((T1)v[0],(T2)v[1],(T3)v[2],(T4)v[3],(T5)v[4],(T6)v[5],(T7)v[6],(T8)v[7],(T9)v[8],(T10)v[9],(T11)v[10]); }});
    }

    public static <B,C,T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,T11,T12> StreamCodec<B,C> composite(
        StreamCodec<? super B,T1> c1, Function<C,T1> g1,
        StreamCodec<? super B,T2> c2, Function<C,T2> g2,
        StreamCodec<? super B,T3> c3, Function<C,T3> g3,
        StreamCodec<? super B,T4> c4, Function<C,T4> g4,
        StreamCodec<? super B,T5> c5, Function<C,T5> g5,
        StreamCodec<? super B,T6> c6, Function<C,T6> g6,
        StreamCodec<? super B,T7> c7, Function<C,T7> g7,
        StreamCodec<? super B,T8> c8, Function<C,T8> g8,
        StreamCodec<? super B,T9> c9, Function<C,T9> g9,
        StreamCodec<? super B,T10> c10, Function<C,T10> g10,
        StreamCodec<? super B,T11> c11, Function<C,T11> g11,
        StreamCodec<? super B,T12> c12, Function<C,T12> g12,
        final Function12<T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,T11,T12,C> factory) {
        return compose(codecs(c1,c2,c3,c4,c5,c6,c7,c8,c9,c10,c11,c12), getters(g1,g2,g3,g4,g5,g6,g7,g8,g9,g10,g11,g12),
            new ArrayFactory<C>() { @SuppressWarnings("unchecked") public C create(Object[] v) {
                return factory.apply((T1)v[0],(T2)v[1],(T3)v[2],(T4)v[3],(T5)v[4],(T6)v[5],(T7)v[6],(T8)v[7],(T9)v[8],(T10)v[9],(T11)v[10],(T12)v[11]); }});
    }

    public static <B,C,T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,T11,T12,T13> StreamCodec<B,C> composite(
        StreamCodec<? super B,T1> c1, Function<C,T1> g1,
        StreamCodec<? super B,T2> c2, Function<C,T2> g2,
        StreamCodec<? super B,T3> c3, Function<C,T3> g3,
        StreamCodec<? super B,T4> c4, Function<C,T4> g4,
        StreamCodec<? super B,T5> c5, Function<C,T5> g5,
        StreamCodec<? super B,T6> c6, Function<C,T6> g6,
        StreamCodec<? super B,T7> c7, Function<C,T7> g7,
        StreamCodec<? super B,T8> c8, Function<C,T8> g8,
        StreamCodec<? super B,T9> c9, Function<C,T9> g9,
        StreamCodec<? super B,T10> c10, Function<C,T10> g10,
        StreamCodec<? super B,T11> c11, Function<C,T11> g11,
        StreamCodec<? super B,T12> c12, Function<C,T12> g12,
        StreamCodec<? super B,T13> c13, Function<C,T13> g13,
        final Function13<T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,T11,T12,T13,C> factory) {
        return compose(codecs(c1,c2,c3,c4,c5,c6,c7,c8,c9,c10,c11,c12,c13), getters(g1,g2,g3,g4,g5,g6,g7,g8,g9,g10,g11,g12,g13),
            new ArrayFactory<C>() { @SuppressWarnings("unchecked") public C create(Object[] v) {
                return factory.apply((T1)v[0],(T2)v[1],(T3)v[2],(T4)v[3],(T5)v[4],(T6)v[5],(T7)v[6],(T8)v[7],(T9)v[8],(T10)v[9],(T11)v[10],(T12)v[11],(T13)v[12]); }});
    }

    public static <B,C,T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,T11,T12,T13,T14> StreamCodec<B,C> composite(
        StreamCodec<? super B,T1> c1, Function<C,T1> g1,
        StreamCodec<? super B,T2> c2, Function<C,T2> g2,
        StreamCodec<? super B,T3> c3, Function<C,T3> g3,
        StreamCodec<? super B,T4> c4, Function<C,T4> g4,
        StreamCodec<? super B,T5> c5, Function<C,T5> g5,
        StreamCodec<? super B,T6> c6, Function<C,T6> g6,
        StreamCodec<? super B,T7> c7, Function<C,T7> g7,
        StreamCodec<? super B,T8> c8, Function<C,T8> g8,
        StreamCodec<? super B,T9> c9, Function<C,T9> g9,
        StreamCodec<? super B,T10> c10, Function<C,T10> g10,
        StreamCodec<? super B,T11> c11, Function<C,T11> g11,
        StreamCodec<? super B,T12> c12, Function<C,T12> g12,
        StreamCodec<? super B,T13> c13, Function<C,T13> g13,
        StreamCodec<? super B,T14> c14, Function<C,T14> g14,
        final Function14<T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,T11,T12,T13,T14,C> factory) {
        return compose(codecs(c1,c2,c3,c4,c5,c6,c7,c8,c9,c10,c11,c12,c13,c14), getters(g1,g2,g3,g4,g5,g6,g7,g8,g9,g10,g11,g12,g13,g14),
            new ArrayFactory<C>() { @SuppressWarnings("unchecked") public C create(Object[] v) {
                return factory.apply((T1)v[0],(T2)v[1],(T3)v[2],(T4)v[3],(T5)v[4],(T6)v[5],(T7)v[6],(T8)v[7],(T9)v[8],(T10)v[9],(T11)v[10],(T12)v[11],(T13)v[12],(T14)v[13]); }});
    }

    public static <B,C,T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,T11,T12,T13,T14,T15> StreamCodec<B,C> composite(
        StreamCodec<? super B,T1> c1, Function<C,T1> g1,
        StreamCodec<? super B,T2> c2, Function<C,T2> g2,
        StreamCodec<? super B,T3> c3, Function<C,T3> g3,
        StreamCodec<? super B,T4> c4, Function<C,T4> g4,
        StreamCodec<? super B,T5> c5, Function<C,T5> g5,
        StreamCodec<? super B,T6> c6, Function<C,T6> g6,
        StreamCodec<? super B,T7> c7, Function<C,T7> g7,
        StreamCodec<? super B,T8> c8, Function<C,T8> g8,
        StreamCodec<? super B,T9> c9, Function<C,T9> g9,
        StreamCodec<? super B,T10> c10, Function<C,T10> g10,
        StreamCodec<? super B,T11> c11, Function<C,T11> g11,
        StreamCodec<? super B,T12> c12, Function<C,T12> g12,
        StreamCodec<? super B,T13> c13, Function<C,T13> g13,
        StreamCodec<? super B,T14> c14, Function<C,T14> g14,
        StreamCodec<? super B,T15> c15, Function<C,T15> g15,
        final Function15<T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,T11,T12,T13,T14,T15,C> factory) {
        return compose(codecs(c1,c2,c3,c4,c5,c6,c7,c8,c9,c10,c11,c12,c13,c14,c15), getters(g1,g2,g3,g4,g5,g6,g7,g8,g9,g10,g11,g12,g13,g14,g15),
            new ArrayFactory<C>() { @SuppressWarnings("unchecked") public C create(Object[] v) {
                return factory.apply((T1)v[0],(T2)v[1],(T3)v[2],(T4)v[3],(T5)v[4],(T6)v[5],(T7)v[6],(T8)v[7],(T9)v[8],(T10)v[9],(T11)v[10],(T12)v[11],(T13)v[12],(T14)v[13],(T15)v[14]); }});
    }

    public static <B,C,T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,T11,T12,T13,T14,T15,T16> StreamCodec<B,C> composite(
        StreamCodec<? super B,T1> c1, Function<C,T1> g1,
        StreamCodec<? super B,T2> c2, Function<C,T2> g2,
        StreamCodec<? super B,T3> c3, Function<C,T3> g3,
        StreamCodec<? super B,T4> c4, Function<C,T4> g4,
        StreamCodec<? super B,T5> c5, Function<C,T5> g5,
        StreamCodec<? super B,T6> c6, Function<C,T6> g6,
        StreamCodec<? super B,T7> c7, Function<C,T7> g7,
        StreamCodec<? super B,T8> c8, Function<C,T8> g8,
        StreamCodec<? super B,T9> c9, Function<C,T9> g9,
        StreamCodec<? super B,T10> c10, Function<C,T10> g10,
        StreamCodec<? super B,T11> c11, Function<C,T11> g11,
        StreamCodec<? super B,T12> c12, Function<C,T12> g12,
        StreamCodec<? super B,T13> c13, Function<C,T13> g13,
        StreamCodec<? super B,T14> c14, Function<C,T14> g14,
        StreamCodec<? super B,T15> c15, Function<C,T15> g15,
        StreamCodec<? super B,T16> c16, Function<C,T16> g16,
        final Function16<T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,T11,T12,T13,T14,T15,T16,C> factory) {
        return compose(codecs(c1,c2,c3,c4,c5,c6,c7,c8,c9,c10,c11,c12,c13,c14,c15,c16), getters(g1,g2,g3,g4,g5,g6,g7,g8,g9,g10,g11,g12,g13,g14,g15,g16),
            new ArrayFactory<C>() { @SuppressWarnings("unchecked") public C create(Object[] v) {
                return factory.apply((T1)v[0],(T2)v[1],(T3)v[2],(T4)v[3],(T5)v[4],(T6)v[5],(T7)v[6],(T8)v[7],(T9)v[8],(T10)v[9],(T11)v[10],(T12)v[11],(T13)v[12],(T14)v[13],(T15)v[14],(T16)v[15]); }});
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <B,C> StreamCodec<B,C> compose(final StreamCodec<?,?>[] codecs,
                                                   final Function<?,?>[] getters,
                                                   final ArrayFactory<C> factory) {
        if (codecs.length != getters.length) throw new IllegalArgumentException("Codec/getter count mismatch");
        for (Object codec : codecs) if (codec == null) throw new IllegalArgumentException("Null field codec");
        for (Object getter : getters) if (getter == null) throw new IllegalArgumentException("Null field getter");
        if (factory == null) throw new IllegalArgumentException("factory");
        return new StreamCodec<B,C>() {
            @Override public C decode(B buffer) {
                Object[] fields = new Object[codecs.length];
                for (int i = 0; i < codecs.length; i++)
                    fields[i] = ((StreamCodec) codecs[i]).decode(buffer);
                return factory.create(fields);
            }
            @Override public void encode(B buffer, C value) {
                if (value == null) throw new IllegalArgumentException("Cannot encode null composite value");
                for (int i = 0; i < codecs.length; i++) {
                    Object field = ((Function) getters[i]).apply(value);
                    ((StreamCodec) codecs[i]).encode(buffer, field);
                }
            }
        };
    }

    private static StreamCodec<?,?>[] codecs(StreamCodec<?,?>... codecs) { return codecs; }
    private static Function<?,?>[] getters(Function<?,?>... getters) { return getters; }
    private interface ArrayFactory<C> { C create(Object[] values); }

    @FunctionalInterface public interface Function7<A,B,C,D,E,F,G,R>{R apply(A a,B b,C c,D d,E e,F f,G g);}
    @FunctionalInterface public interface Function8<A,B,C,D,E,F,G,H,R>{R apply(A a,B b,C c,D d,E e,F f,G g,H h);}
    @FunctionalInterface public interface Function9<A,B,C,D,E,F,G,H,I,R>{R apply(A a,B b,C c,D d,E e,F f,G g,H h,I i);}
    @FunctionalInterface public interface Function10<A,B,C,D,E,F,G,H,I,J,R>{R apply(A a,B b,C c,D d,E e,F f,G g,H h,I i,J j);}
    @FunctionalInterface public interface Function11<A,B,C,D,E,F,G,H,I,J,K,R>{R apply(A a,B b,C c,D d,E e,F f,G g,H h,I i,J j,K k);}
    @FunctionalInterface public interface Function12<A,B,C,D,E,F,G,H,I,J,K,L,R>{R apply(A a,B b,C c,D d,E e,F f,G g,H h,I i,J j,K k,L l);}
    @FunctionalInterface public interface Function13<A,B,C,D,E,F,G,H,I,J,K,L,M,R>{R apply(A a,B b,C c,D d,E e,F f,G g,H h,I i,J j,K k,L l,M m);}
    @FunctionalInterface public interface Function14<A,B,C,D,E,F,G,H,I,J,K,L,M,N,R>{R apply(A a,B b,C c,D d,E e,F f,G g,H h,I i,J j,K k,L l,M m,N n);}
    @FunctionalInterface public interface Function15<A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,R>{R apply(A a,B b,C c,D d,E e,F f,G g,H h,I i,J j,K k,L l,M m,N n,O o);}
    @FunctionalInterface public interface Function16<A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,R>{R apply(A a,B b,C c,D d,E e,F f,G g,H h,I i,J j,K k,L l,M m,N n,O o,P p);}
}
