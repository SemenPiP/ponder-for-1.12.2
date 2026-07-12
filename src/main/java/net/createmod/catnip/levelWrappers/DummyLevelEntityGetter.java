package net.createmod.catnip.levelWrappers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;

/** Small in-memory entity index replacing the modern LevelEntityGetter contract. */
public class DummyLevelEntityGetter<T extends Entity> {
    private final Map<Integer, T> byId = new LinkedHashMap<Integer, T>();
    private final Map<UUID, T> byUuid = new LinkedHashMap<UUID, T>();

    public synchronized boolean add(T entity) {
        if (entity == null) throw new IllegalArgumentException("entity");
        T existingId = byId.get(entity.getEntityId());
        T existingUuid = byUuid.get(entity.getUniqueID());
        if (existingId == entity && existingUuid == entity) return false;
        if (existingId != null) remove(existingId);
        if (existingUuid != null) remove(existingUuid);
        byId.put(entity.getEntityId(), entity);
        byUuid.put(entity.getUniqueID(), entity);
        return true;
    }

    public synchronized boolean remove(T entity) {
        if (entity == null) return false;
        boolean removed = byId.remove(entity.getEntityId()) != null;
        byUuid.remove(entity.getUniqueID());
        return removed;
    }

    @Nullable public synchronized T get(int id) { return byId.get(id); }
    @Nullable public synchronized T get(UUID uuid) { return byUuid.get(uuid); }
    public synchronized Iterable<T> getAll() {
        return Collections.unmodifiableList(new ArrayList<T>(byId.values()));
    }
    public synchronized int size() { return byId.size(); }
    public synchronized void clear() { byId.clear(); byUuid.clear(); }

    public synchronized void get(AxisAlignedBB bounds, Consumer<T> consumer) {
        if (bounds == null || consumer == null) throw new IllegalArgumentException("bounds/consumer");
        for (T entity : byId.values())
            if (entity.getEntityBoundingBox().intersects(bounds)) consumer.accept(entity);
    }

    public synchronized <U extends T> List<U> get(Class<U> type, AxisAlignedBB bounds,
                                                   Predicate<? super U> predicate) {
        if (type == null || bounds == null || predicate == null)
            throw new IllegalArgumentException("type/bounds/predicate");
        List<U> result = new ArrayList<U>();
        for (T entity : byId.values()) {
            if (!type.isInstance(entity) || !entity.getEntityBoundingBox().intersects(bounds)) continue;
            U cast = type.cast(entity);
            if (predicate.test(cast)) result.add(cast);
        }
        return Collections.unmodifiableList(result);
    }
}
