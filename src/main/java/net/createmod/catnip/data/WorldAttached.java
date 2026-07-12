package net.createmod.catnip.data;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import net.minecraft.world.World;

public class WorldAttached<T> {
    private static final List<WeakReference<Map<World, ?>>> ALL_MAPS = new ArrayList<WeakReference<Map<World, ?>>>();
    private final Map<World, T> attached = new WeakHashMap<World, T>();
    private final Function<World, T> factory;
    public WorldAttached(Function<World, T> factory) {
        if (factory == null) throw new NullPointerException("factory");
        this.factory = factory;
        synchronized (ALL_MAPS) { ALL_MAPS.add(new WeakReference<Map<World, ?>>(attached)); }
    }
    public static void invalidateWorld(World world) {
        synchronized (ALL_MAPS) {
            Iterator<WeakReference<Map<World, ?>>> iterator = ALL_MAPS.iterator();
            while (iterator.hasNext()) {
                Map<World, ?> map = iterator.next().get();
                if (map == null) iterator.remove(); else map.remove(world);
            }
        }
    }
    public synchronized T get(World world) {
        T value = attached.get(world);
        if (value == null) { value = factory.apply(world); attached.put(world, value); }
        return value;
    }
    public synchronized void put(World world, T value) { attached.put(world, value); }
    public synchronized T replace(World world) { attached.remove(world); return get(world); }
    public synchronized T replace(World world, Consumer<T> finalizer) {
        T old = attached.remove(world); if (old != null) finalizer.accept(old); return get(world);
    }
    public synchronized void empty(BiConsumer<World, T> finalizer) {
        for (Map.Entry<World, T> entry : attached.entrySet()) finalizer.accept(entry.getKey(), entry.getValue());
        attached.clear();
    }
    public synchronized void empty(Consumer<T> finalizer) {
        for (T value : attached.values()) finalizer.accept(value); attached.clear();
    }
}
