package net.createmod.ponder.api.level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Public compatibility name for Ponder's in-memory 1.12.2 world. */
public class PonderLevel extends PonderWorld {
    private Map<String, List<BlockPos>> structureGroups = Collections.emptyMap();

    public PonderLevel(BlockPos anchor, @Nullable World original) {
        super(anchor, original);
    }

    public void createBackup() {
        backup();
    }

    public void setStructureGroups(Map<String, List<BlockPos>> groups) {
        if (groups == null || groups.isEmpty()) {
            structureGroups = Collections.emptyMap();
            return;
        }
        Map<String, List<BlockPos>> copy = new LinkedHashMap<String, List<BlockPos>>();
        for (Map.Entry<String, List<BlockPos>> entry : groups.entrySet()) {
            List<BlockPos> positions = new ArrayList<BlockPos>();
            for (BlockPos position : entry.getValue())
                positions.add(position.toImmutable());
            copy.put(entry.getKey(), Collections.unmodifiableList(positions));
        }
        structureGroups = Collections.unmodifiableMap(copy);
    }

    public Map<String, List<BlockPos>> getStructureGroups() {
        return structureGroups;
    }

    @Override
    public void restore() {
        super.restore();
        PonderIndex.forEachPlugin(plugin -> plugin.onPonderLevelRestore(this));
    }
}
