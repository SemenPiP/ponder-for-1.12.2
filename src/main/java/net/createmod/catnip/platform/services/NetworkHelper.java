package net.createmod.catnip.platform.services;

import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.CatnipPacketRegistry;
import net.createmod.catnip.net.packets.ClientboundSimpleActionPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;

public interface NetworkHelper {
    void registerPackets(CatnipPacketRegistry registry); void sendToServer(BasePacketPayload payload); void sendToClient(EntityPlayerMP player,BasePacketPayload payload);
    void sendToAllClients(BasePacketPayload payload); void sendToClientsTrackingAndSelf(Entity entity,BasePacketPayload payload);
    void sendToClientsTrackingEntity(Entity entity,BasePacketPayload payload); void sendToClientsTrackingChunk(WorldServer world,ChunkPos chunk,BasePacketPayload payload);
    void sendToClientsAround(WorldServer world,Vec3d pos,double radius,BasePacketPayload payload);
    default void sendToClients(Iterable<EntityPlayerMP> players,BasePacketPayload payload){for(EntityPlayerMP player:players)sendToClient(player,payload);}
    default void sendToClientsAround(WorldServer world,BlockPos pos,double radius,BasePacketPayload payload){sendToClientsAround(world,new Vec3d(pos),radius,payload);}
    default void simpleActionToClient(EntityPlayerMP player,String action,String value){sendToClient(player,new ClientboundSimpleActionPacket(action,value));}
}
