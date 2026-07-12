package net.createmod.ponder.api.level;

import javax.annotation.Nullable;

import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Public compatibility name for Ponder's in-memory 1.12.2 world. */
public class PonderLevel extends PonderWorld {
    public PonderLevel(BlockPos anchor, @Nullable World original) {
        super(anchor, original);
    }

    public void createBackup() {
        backup();
    }

    @Override
    public void restore() {
        super.restore();
        PonderIndex.forEachPlugin(plugin -> plugin.onPonderLevelRestore(this));
    }
}
