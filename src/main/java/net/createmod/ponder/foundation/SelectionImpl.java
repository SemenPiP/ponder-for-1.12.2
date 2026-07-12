package net.createmod.ponder.foundation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.createmod.catnip.outliner.Outline;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** Deterministic, finite selection implementation used by the public scene DSL. */
public final class SelectionImpl implements Selection {
    private final LinkedHashSet<BlockPos> positions;
    private Vec3d cachedCenter;

    private SelectionImpl(LinkedHashSet<BlockPos> positions) {
        this.positions = positions;
    }

    public static Selection empty() {
        return new SelectionImpl(new LinkedHashSet<BlockPos>());
    }

    public static Selection of(BlockPos position) {
        LinkedHashSet<BlockPos> result = new LinkedHashSet<BlockPos>();
        result.add(position.toImmutable());
        return new SelectionImpl(result);
    }

    public static Selection of(BlockPos first, BlockPos second) {
        int minX = Math.min(first.getX(), second.getX());
        int minY = Math.min(first.getY(), second.getY());
        int minZ = Math.min(first.getZ(), second.getZ());
        int maxX = Math.max(first.getX(), second.getX());
        int maxY = Math.max(first.getY(), second.getY());
        int maxZ = Math.max(first.getZ(), second.getZ());
        LinkedHashSet<BlockPos> result = new LinkedHashSet<BlockPos>();
        for (int y = minY; y <= maxY; y++)
            for (int z = minZ; z <= maxZ; z++)
                for (int x = minX; x <= maxX; x++)
                    result.add(new BlockPos(x, y, z));
        return new SelectionImpl(result);
    }

    public static Selection of(AxisAlignedBB bounds) {
        return of(new BlockPos(Math.floor(bounds.minX), Math.floor(bounds.minY), Math.floor(bounds.minZ)),
            new BlockPos(Math.ceil(bounds.maxX) - 1, Math.ceil(bounds.maxY) - 1, Math.ceil(bounds.maxZ) - 1));
    }

    @Override
    public boolean test(BlockPos pos) {
        return pos != null && positions.contains(pos);
    }

    @Override
    public Selection add(Selection other) {
        if (other != null)
            for (BlockPos pos : other)
                positions.add(pos.toImmutable());
        cachedCenter = null;
        return this;
    }

    @Override
    public Selection substract(Selection other) {
        if (other != null)
            for (BlockPos pos : other)
                positions.remove(pos);
        cachedCenter = null;
        return this;
    }

    @Override
    public Selection copy() {
        return new SelectionImpl(new LinkedHashSet<BlockPos>(positions));
    }

    @Override
    public Vec3d getCenter() {
        if (cachedCenter != null)
            return cachedCenter;
        if (positions.isEmpty())
            return Vec3d.ZERO;
        double x = 0;
        double y = 0;
        double z = 0;
        for (BlockPos pos : positions) {
            x += pos.getX() + .5;
            y += pos.getY() + .5;
            z += pos.getZ() + .5;
        }
        cachedCenter = new Vec3d(x / positions.size(), y / positions.size(), z / positions.size());
        return cachedCenter;
    }

    @Override
    public boolean isEmpty() {
        return positions.isEmpty();
    }

    @Override
    public int size() {
        return positions.size();
    }

    @Override
    public Iterator<BlockPos> iterator() {
        List<BlockPos> snapshot = new ArrayList<BlockPos>(positions);
        return Collections.unmodifiableList(snapshot).iterator();
    }

    public Set<BlockPos> asSet() {
        return Collections.unmodifiableSet(positions);
    }

    @Override
    public Outline.OutlineParams makeOutline(Outliner outliner, Object slot) {
        if (outliner == null)
            throw new IllegalArgumentException("Outliner is required");
        return outliner.showCluster(slot, positions);
    }
}
