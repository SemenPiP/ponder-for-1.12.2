package net.createmod.catnip.levelWrappers;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.util.math.ChunkPos;

/** Observable chunk-progress tracker for 1.12 code that has no ChunkProgressListener API. */
public class DummyStatusListener {
    public enum Status { QUEUED, LOADING, COMPLETE, FAILED }

    private final Map<Long, Status> statuses = new LinkedHashMap<Long, Status>();
    @Nullable private ChunkPos spawnPosition;
    private boolean running;

    public synchronized void updateSpawnPos(ChunkPos center) {
        if (center == null) throw new IllegalArgumentException("center");
        spawnPosition = new ChunkPos(center.x, center.z);
    }

    public synchronized void onStatusChange(ChunkPos position, @Nullable Status status) {
        if (position == null) throw new IllegalArgumentException("position");
        long key = key(position.x, position.z);
        if (status == null) statuses.remove(key); else statuses.put(key, status);
    }

    public synchronized void start() { running = true; }
    public synchronized void stop() { running = false; }
    public synchronized boolean isRunning() { return running; }
    @Nullable public synchronized ChunkPos getSpawnPosition() {
        return spawnPosition == null ? null : new ChunkPos(spawnPosition.x, spawnPosition.z);
    }
    public synchronized Map<Long, Status> getStatuses() {
        return Collections.unmodifiableMap(new LinkedHashMap<Long, Status>(statuses));
    }
    @Nullable public synchronized Status getStatus(ChunkPos position) {
        return statuses.get(key(position.x, position.z));
    }

    private static long key(int x, int z) { return x & 0xffffffffL | (z & 0xffffffffL) << 32; }
}
