package net.createmod.catnip.codecs.stream;

import java.util.function.Function;

public interface StreamCodec<B, T> {
    T decode(B buffer);
    void encode(B buffer, T value);

    default <R> StreamCodec<B, R> map(final Function<T, R> decoder, final Function<R, T> encoder) {
        final StreamCodec<B, T> self = this;
        return new StreamCodec<B, R>() {
            public R decode(B buffer) { return decoder.apply(self.decode(buffer)); }
            public void encode(B buffer, R value) { self.encode(buffer, encoder.apply(value)); }
        };
    }
}
