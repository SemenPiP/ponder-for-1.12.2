package net.createmod.catnip.placement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import net.createmod.catnip.ghostblock.GhostBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public interface IPlacementHelper {
    Predicate<ItemStack> getItemPredicate();
    Predicate<IBlockState> getStatePredicate();

    PlacementOffset getOffset(EntityPlayer player, World world, IBlockState state,
                              BlockPos pos, RayTraceResult ray);

    default PlacementOffset getOffset(EntityPlayer player, World world, IBlockState state,
                                      BlockPos pos, RayTraceResult ray, ItemStack heldItem) {
        PlacementOffset result = getOffset(player, world, state, pos, ray);
        if (heldItem.getItem() instanceof ItemBlock) {
            result.withGhostState(((ItemBlock) heldItem.getItem()).getBlock().getDefaultState());
        }
        return result;
    }

    default boolean matchesItem(ItemStack stack) { return getItemPredicate().test(stack); }
    default boolean matchesState(IBlockState state) { return getStatePredicate().test(state); }

    default void displayGhost(PlacementOffset offset) {
        if (offset.isSuccessful() && offset.hasGhostState()) {
            GhostBlocks.getInstance().showGhostState(this,
                offset.getTransform().apply(offset.getGhostState()), 2)
                .at(offset.getBlockPos()).breathingAlpha();
        }
    }

    static List<EnumFacing> orderedByDistance(BlockPos pos, Vec3d hit,
                                              Collection<EnumFacing> directions) {
        final Vec3d center = new Vec3d(pos).add(.5, .5, .5);
        List<EnumFacing> result = new ArrayList<EnumFacing>(directions);
        result.sort(new Comparator<EnumFacing>() {
            @Override
            public int compare(EnumFacing first, EnumFacing second) {
                Vec3d firstPoint = center.add(new Vec3d(first.getDirectionVec()));
                Vec3d secondPoint = center.add(new Vec3d(second.getDirectionVec()));
                return Double.compare(firstPoint.squareDistanceTo(hit), secondPoint.squareDistanceTo(hit));
            }
        });
        return result;
    }
}
