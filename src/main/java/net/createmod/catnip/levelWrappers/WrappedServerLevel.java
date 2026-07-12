package net.createmod.catnip.levelWrappers;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

/** WorldServer delegate used by server-side simulations that need a stable 1.12 type. */
public class WrappedServerLevel extends WrappedLevel {
    protected final WorldServer serverLevel;

    public WrappedServerLevel(WorldServer level) {
        super(level);
        serverLevel = level;
    }

    public WorldServer getServerLevel() { return serverLevel; }
    public void scheduleUpdate(BlockPos pos, Block block, int delay) {
        serverLevel.scheduleUpdate(pos, block, delay);
    }
    @Override public boolean spawnEntity(Entity entity) { return serverLevel.spawnEntity(entity); }
}
