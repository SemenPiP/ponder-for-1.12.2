package net.createmod.catnip.placement;

import java.util.function.UnaryOperator;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class PlacementOffset {
    private final boolean successful;
    private BlockPos pos = BlockPos.ORIGIN;
    private UnaryOperator<IBlockState> transform = UnaryOperator.identity();
    @Nullable
    private IBlockState ghostState;

    private PlacementOffset(boolean successful) {
        this.successful = successful;
    }

    public static PlacementOffset fail() { return new PlacementOffset(false); }
    public static PlacementOffset success() { return new PlacementOffset(true); }
    public static PlacementOffset success(BlockPos pos) { return success().at(pos); }

    public PlacementOffset at(BlockPos pos) {
        this.pos = pos.toImmutable();
        return this;
    }

    public PlacementOffset withTransform(UnaryOperator<IBlockState> transform) {
        this.transform = transform == null ? UnaryOperator.<IBlockState>identity() : transform;
        return this;
    }

    public PlacementOffset withGhostState(IBlockState ghostState) {
        this.ghostState = ghostState;
        return this;
    }

    public boolean isSuccessful() { return successful; }
    public BlockPos getBlockPos() { return pos; }
    public UnaryOperator<IBlockState> getTransform() { return transform; }
    public boolean hasGhostState() { return ghostState != null; }
    @Nullable public IBlockState getGhostState() { return ghostState; }

    public boolean isReplaceable(World world) {
        IBlockState existing = world.getBlockState(pos);
        return successful && (existing.getBlock() == Blocks.AIR
            || existing.getBlock().isReplaceable(world, pos));
    }

    public EnumActionResult placeInWorld(World world, ItemStack held, EntityPlayer player, EnumHand hand) {
        if (!successful || held.isEmpty() || !(held.getItem() instanceof ItemBlock) || !isReplaceable(world)) {
            return EnumActionResult.PASS;
        }
        if (!world.isBlockModifiable(player, pos)) {
            return EnumActionResult.FAIL;
        }
        ItemBlock itemBlock = (ItemBlock) held.getItem();
        Block block = itemBlock.getBlock();
        IBlockState state = transform.apply(block.getDefaultState());
        if (state == null || !state.getBlock().canPlaceBlockAt(world, pos)) {
            return EnumActionResult.FAIL;
        }
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }
        if (!world.setBlockState(pos, state, 3)) {
            return EnumActionResult.FAIL;
        }
        IBlockState placed = world.getBlockState(pos);
        block.onBlockPlacedBy(world, pos, placed, player, held.copy());
        world.playSound(null, pos, block.getSoundType(placed, world, pos, player).getPlaceSound(),
            SoundCategory.BLOCKS, .8f, .8f);
        if (!player.capabilities.isCreativeMode) {
            held.shrink(1);
        }
        return EnumActionResult.SUCCESS;
    }
}
