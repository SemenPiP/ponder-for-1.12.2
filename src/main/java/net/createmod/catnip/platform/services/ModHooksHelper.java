package net.createmod.catnip.platform.services;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

public interface ModHooksHelper {
    boolean playerPlaceSingleBlock(EntityPlayer player,World world,BlockPos pos,IBlockState newState);
    ItemStack getCloneItemFromBlockstate(IBlockState state,RayTraceResult target,World world,BlockPos pos,EntityPlayer player);
    boolean isPlayerFake(EntityPlayerMP player);
}
