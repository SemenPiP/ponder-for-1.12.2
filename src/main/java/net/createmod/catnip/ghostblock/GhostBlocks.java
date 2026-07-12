package net.createmod.catnip.ghostblock;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.Vec3d;

public final class GhostBlocks {
    private static final GhostBlocks INSTANCE = new GhostBlocks();
    private final Map<Object, Entry> ghosts = new LinkedHashMap<Object, Entry>();

    private GhostBlocks() {}
    public static GhostBlocks getInstance() { return INSTANCE; }

    public static double getBreathingAlpha() {
        double phase = System.currentTimeMillis() % 2500L / 2500d * Math.PI * 2;
        return .55d - .2d * Math.cos(phase);
    }

    public GhostBlockParams showGhostState(Object slot, IBlockState state) {
        return showGhostState(slot, state, 1);
    }

    public GhostBlockParams showGhostState(Object slot, IBlockState state, int ttl) {
        return showGhost(slot, GhostBlockRenderer.transparent(), GhostBlockParams.of(state), ttl);
    }

    public synchronized GhostBlockParams showGhost(Object slot, GhostBlockRenderer renderer,
                                                    GhostBlockParams params, int ttl) {
        if (slot == null || renderer == null || params == null) throw new IllegalArgumentException();
        Entry entry = ghosts.get(slot);
        if (entry == null) {
            entry = new Entry();
            ghosts.put(slot, entry);
        }
        entry.renderer = renderer;
        entry.params = params;
        entry.ticksRemaining = Math.max(1, ttl);
        return params;
    }

    public synchronized void tickGhosts() {
        Iterator<Entry> iterator = ghosts.values().iterator();
        while (iterator.hasNext()) {
            if (--iterator.next().ticksRemaining <= 0) iterator.remove();
        }
    }

    public synchronized void renderAll(Vec3d camera) {
        for (Entry entry : ghosts.values()) entry.renderer.render(camera, entry.params);
    }

    public synchronized int size() { return ghosts.size(); }
    public synchronized void clear() { ghosts.clear(); }

    private static final class Entry {
        private GhostBlockRenderer renderer;
        private GhostBlockParams params;
        private int ticksRemaining;
    }
}
