package net.createmod.catnip.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

public final class SuperByteBufferCache {
    private static final long NANOS_PER_TICK = TimeUnit.MILLISECONDS.toNanos(50);
    private static final SuperByteBufferCache INSTANCE =
        new SuperByteBufferCache(new LongSupplier() {
            @Override
            public long getAsLong() {
                return System.nanoTime();
            }
        });

    private final Map<Compartment<?>, CompartmentCache> caches =
        new HashMap<Compartment<?>, CompartmentCache>();
    private final LongSupplier nanoTime;

    private SuperByteBufferCache() {
        this(new LongSupplier() {
            @Override
            public long getAsLong() {
                return System.nanoTime();
            }
        });
    }

    SuperByteBufferCache(LongSupplier nanoTime) {
        if (nanoTime == null) throw new IllegalArgumentException("nanoTime");
        this.nanoTime = nanoTime;
    }

    public static SuperByteBufferCache getInstance() {
        return INSTANCE;
    }

    public synchronized void registerCompartment(Compartment<?> compartment) {
        register(compartment, -1);
    }

    public synchronized void registerCompartment(Compartment<?> compartment, long ticksUntilExpired) {
        if (ticksUntilExpired < 0) {
            throw new IllegalArgumentException("ticksUntilExpired must not be negative");
        }
        register(compartment, expirationNanos(ticksUntilExpired));
    }

    public synchronized <T> SuperByteBuffer get(Compartment<T> compartment, T key,
                                                 Callable<SuperByteBuffer> factory) {
        if (key == null) throw new IllegalArgumentException("key");
        if (factory == null) throw new IllegalArgumentException("factory");
        CompartmentCache cache = caches.get(compartment);
        if (cache == null) {
            throw new IllegalArgumentException("Unregistered buffer compartment: " + compartment);
        }

        long now = nanoTime.getAsLong();
        removeExpired(cache, now);
        CacheEntry entry = cache.entries.get(key);
        if (entry != null) {
            entry.lastAccessNanos = now;
            return entry.buffer.reset();
        }

        SuperByteBuffer value;
        try {
            value = factory.call();
        } catch (Exception e) {
            throw new IllegalStateException("Could not build buffer for " + key, e);
        }
        if (value == null) {
            throw new IllegalStateException("Buffer factory returned null for " + key);
        }
        cache.entries.put(key, new CacheEntry(value, now));
        return value.reset();
    }

    public synchronized <T> void invalidate(Compartment<T> compartment, T key) {
        CompartmentCache cache = caches.get(compartment);
        if (cache != null) delete(cache.entries.remove(key));
    }

    public synchronized void invalidate(Compartment<?> compartment) {
        CompartmentCache cache = caches.get(compartment);
        if (cache != null) clear(cache);
    }

    public synchronized void invalidate() {
        for (Compartment<?> compartment : new ArrayList<Compartment<?>>(caches.keySet())) {
            invalidate(compartment);
        }
    }

    /** Performs the same maintenance that a cache access would trigger. */
    public synchronized void cleanUp() {
        long now = nanoTime.getAsLong();
        for (CompartmentCache cache : caches.values()) removeExpired(cache, now);
    }

    private void register(Compartment<?> compartment, long expirationNanos) {
        if (compartment == null) throw new IllegalArgumentException("compartment");
        CompartmentCache replaced = caches.put(compartment, new CompartmentCache(expirationNanos));
        if (replaced != null) clear(replaced);
    }

    private static long expirationNanos(long ticks) {
        if (ticks == 0) return 0;
        if (ticks > Long.MAX_VALUE / NANOS_PER_TICK) return Long.MAX_VALUE;
        return ticks * NANOS_PER_TICK;
    }

    private static void removeExpired(CompartmentCache cache, long now) {
        if (cache.expirationNanos < 0) return;
        java.util.Iterator<Map.Entry<Object, CacheEntry>> iterator = cache.entries.entrySet().iterator();
        while (iterator.hasNext()) {
            CacheEntry entry = iterator.next().getValue();
            if (now - entry.lastAccessNanos >= cache.expirationNanos) {
                iterator.remove();
                delete(entry);
            }
        }
    }

    private static void clear(CompartmentCache cache) {
        for (CacheEntry entry : cache.entries.values()) delete(entry);
        cache.entries.clear();
    }

    private static void delete(CacheEntry entry) {
        if (entry != null) entry.buffer.delete();
    }

    private static final class CompartmentCache {
        private final Map<Object, CacheEntry> entries = new LinkedHashMap<Object, CacheEntry>();
        private final long expirationNanos;

        private CompartmentCache(long expirationNanos) {
            this.expirationNanos = expirationNanos;
        }
    }

    private static final class CacheEntry {
        private final SuperByteBuffer buffer;
        private long lastAccessNanos;

        private CacheEntry(SuperByteBuffer buffer, long lastAccessNanos) {
            this.buffer = buffer;
            this.lastAccessNanos = lastAccessNanos;
        }
    }

    public static final class Compartment<T> {
    }
}
