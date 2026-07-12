package net.createmod.catnip.placement;

import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;

public final class PlacementClient {
    private static float currentAlpha;
    @Nullable
    private static PlacementOffset currentOffset;

    private PlacementClient() {}

    public static void tick() {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.player;
        RayTraceResult hit = minecraft.objectMouseOver;
        currentOffset = null;
        if (player == null || minecraft.world == null || hit == null
            || hit.typeOfHit != RayTraceResult.Type.BLOCK) {
            currentAlpha += (0f - currentAlpha) * .35f;
            return;
        }
        BlockPos pos = hit.getBlockPos();
        IBlockState state = minecraft.world.getBlockState(pos);
        ItemStack held = player.getHeldItemMainhand();
        for (IPlacementHelper helper : PlacementHelpers.getHelpersView()) {
            if (!helper.matchesItem(held) || !helper.matchesState(state)) {
                continue;
            }
            PlacementOffset candidate = helper.getOffset(player, minecraft.world, state, pos, hit, held);
            if (candidate.isSuccessful() && candidate.isReplaceable(minecraft.world)) {
                currentOffset = candidate;
                helper.displayGhost(candidate);
                break;
            }
        }
        currentAlpha += ((currentOffset == null ? 0f : 1f) - currentAlpha) * .35f;
    }

    public static float getCurrentAlpha() { return currentAlpha; }
    @Nullable public static PlacementOffset getCurrentOffset() { return currentOffset; }
}
