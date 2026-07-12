package net.createmod.catnip.codecs.stream;

import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.data.Pair;

public final class CatnipStreamCodecBuilders {
    public static final int DEFAULT_MAX_COLLECTION = 4096;
    public static final int DEFAULT_MAX_STRING_BYTES = 32767;
    private CatnipStreamCodecBuilders() {}

    public static <B extends ByteBuf, E extends Enum<E>> StreamCodec<B, E> ofEnum(final Class<E> type) {
        final E[] values = type.getEnumConstants();
        if (values == null || values.length == 0) throw new IllegalArgumentException(type + " is not an enum");
        return new StreamCodec<B, E>() {
            public E decode(B buffer) {
                int ordinal = readVarInt(buffer);
                if (ordinal < 0 || ordinal >= values.length) throw new IllegalArgumentException("Invalid " + type.getSimpleName() + " ordinal: " + ordinal);
                return values[ordinal];
            }
            public void encode(B buffer, E value) { writeVarInt(buffer, value.ordinal()); }
        };
    }

    public static <B extends ByteBuf, L, R> StreamCodec<B, Pair<L, R>> pair(
            final StreamCodec<? super B, L> left, final StreamCodec<? super B, R> right) {
        return Pair.streamCodec(left, right);
    }

    public static <B extends ByteBuf, V> StreamCodec<B, V> nullable(final StreamCodec<? super B, V> base) {
        return new StreamCodec<B, V>() {
            public V decode(B buffer) { return buffer.readBoolean() ? base.decode(buffer) : null; }
            public void encode(B buffer, V value) { buffer.writeBoolean(value != null); if (value != null) base.encode(buffer, value); }
        };
    }

    public static <B extends ByteBuf, V> StreamCodec<B, Optional<V>> optional(final StreamCodec<? super B, V> base) {
        return new StreamCodec<B, Optional<V>>() {
            public Optional<V> decode(B buffer) { return buffer.readBoolean() ? Optional.of(base.decode(buffer)) : Optional.<V>empty(); }
            public void encode(B buffer, Optional<V> value) { buffer.writeBoolean(value.isPresent()); if (value.isPresent()) base.encode(buffer, value.get()); }
        };
    }

    public static <B extends ByteBuf, V> StreamCodec<B, List<V>> list(StreamCodec<? super B, V> base) {
        return list(base, DEFAULT_MAX_COLLECTION);
    }

    public static <B extends ByteBuf, V> StreamCodec<B, List<V>> list(final StreamCodec<? super B, V> base, final int maxSize) {
        if (maxSize < 0) throw new IllegalArgumentException("maxSize cannot be negative");
        return new StreamCodec<B, List<V>>() {
            public List<V> decode(B buffer) {
                int size = readVarInt(buffer);
                if (size < 0 || size > maxSize) throw new IllegalArgumentException("Collection size " + size + " exceeds " + maxSize);
                List<V> result = new ArrayList<V>(size);
                for (int i = 0; i < size; i++) result.add(base.decode(buffer));
                return result;
            }
            public void encode(B buffer, List<V> values) {
                if (values.size() > maxSize) throw new IllegalArgumentException("Collection size " + values.size() + " exceeds " + maxSize);
                writeVarInt(buffer, values.size());
                for (V value : values) base.encode(buffer, value);
            }
        };
    }

    public static <B extends ByteBuf, V> StreamCodec<B, V[]> array(final StreamCodec<? super B, V> base, final Class<V> type, final int maxSize) {
        return new StreamCodec<B, V[]>() {
            @SuppressWarnings("unchecked") public V[] decode(B buffer) {
                int size = readVarInt(buffer);
                if (size < 0 || size > maxSize) throw new IllegalArgumentException("Array size " + size + " exceeds " + maxSize);
                V[] values = (V[]) Array.newInstance(type, size);
                for (int i = 0; i < size; i++) values[i] = base.decode(buffer);
                return values;
            }
            public void encode(B buffer, V[] values) {
                if (values.length > maxSize) throw new IllegalArgumentException("Array size " + values.length + " exceeds " + maxSize);
                writeVarInt(buffer, values.length);
                for (V value : values) base.encode(buffer, value);
            }
        };
    }

    public static <B extends ByteBuf> StreamCodec<B, String> string(final int maxBytes) {
        return new StreamCodec<B, String>() {
            public String decode(B buffer) {
                int length = readVarInt(buffer);
                if (length < 0 || length > maxBytes || length > buffer.readableBytes())
                    throw new IllegalArgumentException("Invalid UTF-8 length: " + length);
                String result = buffer.toString(buffer.readerIndex(), length, StandardCharsets.UTF_8);
                buffer.skipBytes(length);
                if (result.length() > maxBytes) throw new IllegalArgumentException("Decoded string is too long");
                return result;
            }
            public void encode(B buffer, String value) {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                if (bytes.length > maxBytes) throw new IllegalArgumentException("UTF-8 string exceeds " + maxBytes + " bytes");
                writeVarInt(buffer, bytes.length); buffer.writeBytes(bytes);
            }
        };
    }

    public static void writeVarInt(ByteBuf buffer, int value) {
        while ((value & -128) != 0) { buffer.writeByte(value & 127 | 128); value >>>= 7; }
        buffer.writeByte(value);
    }
    public static int readVarInt(ByteBuf buffer) {
        int result = 0;
        int bytes = 0;
        byte current;
        do {
            if (!buffer.isReadable()) throw new IllegalArgumentException("Truncated VarInt");
            current = buffer.readByte();
            result |= (current & 127) << (bytes++ * 7);
            if (bytes > 5) throw new IllegalArgumentException("VarInt is too large");
        } while ((current & 128) != 0);
        return result;
    }
}
