package net.createmod.ponder.foundation.instruction;

import java.util.function.UnaryOperator;

import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.PonderWorld;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;

public final class ReplaceBlocksInstruction extends CallbackInstruction {
    public ReplaceBlocksInstruction(final Selection selection, final UnaryOperator<IBlockState> operator,
                                    final boolean includeAir, final boolean spawnParticles) {
        super(scene -> {
            PonderWorld world = scene.getWorld();
            if (world == null) return;
            for (BlockPos pos : selection) {
                IBlockState old = world.getBlockState(pos);
                if (!includeAir && old.getBlock().isAir(old, world, pos)) continue;
                IBlockState replacement = operator.apply(old);
                if (replacement == null) continue;
                if (spawnParticles) {
                    int blockId = Block.getIdFromBlock(old.getBlock());
                    int meta = old.getBlock().getMetaFromState(old);
                    world.spawnParticle(EnumParticleTypes.BLOCK_CRACK, pos.getX() + .5, pos.getY() + .5,
                        pos.getZ() + .5, 0, .05, 0, blockId | meta << 12);
                }
                world.setBlockState(pos, replacement, 2);
            }
            scene.forEach(WorldSectionElement.class, WorldSectionElement::queueRedraw);
        });
    }
}
