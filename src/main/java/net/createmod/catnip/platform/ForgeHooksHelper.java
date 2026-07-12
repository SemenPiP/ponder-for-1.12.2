package net.createmod.catnip.platform;

import net.createmod.catnip.platform.services.ModHooksHelper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.world.BlockEvent;

public class ForgeHooksHelper implements ModHooksHelper {
    public boolean playerPlaceSingleBlock(EntityPlayer player,World world,BlockPos pos,IBlockState newState){
        BlockSnapshot snapshot=BlockSnapshot.getBlockSnapshot(world,pos);
        IBlockState against=world.getBlockState(pos.down());
        if(!world.setBlockState(pos,newState,3))return true;
        BlockEvent.PlaceEvent event=new BlockEvent.PlaceEvent(snapshot,against,player,EnumHand.MAIN_HAND);
        if(MinecraftForge.EVENT_BUS.post(event)){snapshot.restore(true,true);return true;}
        return false;
    }
    public ItemStack getCloneItemFromBlockstate(IBlockState state,RayTraceResult target,World world,BlockPos pos,EntityPlayer player){return state.getBlock().getPickBlock(state,target,world,pos,player);}
    public boolean isPlayerFake(EntityPlayerMP player){return player instanceof FakePlayer;}
}
