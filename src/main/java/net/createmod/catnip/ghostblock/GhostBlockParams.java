package net.createmod.catnip.ghostblock;

import java.util.function.Supplier;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;

public final class GhostBlockParams {
    private final IBlockState state;
    private BlockPos pos = BlockPos.ORIGIN;
    private Supplier<Float> alphaSupplier = new Supplier<Float>() {
        @Override public Float get() { return 1f; }
    };

    private GhostBlockParams(IBlockState state) {
        if (state == null) throw new IllegalArgumentException("state");
        this.state = state;
    }

    public static GhostBlockParams of(IBlockState state) { return new GhostBlockParams(state); }
    public static GhostBlockParams of(Block block) { return of(block.getDefaultState()); }
    public GhostBlockParams at(BlockPos pos) { this.pos = pos.toImmutable(); return this; }
    public GhostBlockParams at(int x, int y, int z) { return at(new BlockPos(x, y, z)); }
    public GhostBlockParams alpha(Supplier<Float> alpha) { this.alphaSupplier = alpha; return this; }
    public GhostBlockParams alpha(final float alpha) {
        return alpha(new Supplier<Float>() { @Override public Float get() { return alpha; } });
    }
    public GhostBlockParams breathingAlpha() {
        return alpha(new Supplier<Float>() {
            @Override public Float get() { return (float) GhostBlocks.getBreathingAlpha(); }
        });
    }
    public IBlockState getState() { return state; }
    public BlockPos getPos() { return pos; }
    public float getAlpha() { return Math.max(0, Math.min(1, alphaSupplier.get())); }
}
