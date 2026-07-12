package net.createmod.catnip.platform;

import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.CatnipPacketRegistry;
import net.createmod.catnip.platform.services.NetworkHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

public class ForgeNetworkHelper implements NetworkHelper {
    public void registerPackets(CatnipPacketRegistry registry){registry.registerInternal();}
    private SimpleNetworkWrapper channel(BasePacketPayload payload){return payload.getTypeProvider().getPacketType().getOwner().getChannel();}
    public void sendToServer(BasePacketPayload payload){channel(payload).sendToServer(payload);}
    public void sendToClient(EntityPlayerMP player,BasePacketPayload payload){channel(payload).sendTo(payload,player);}
    public void sendToAllClients(BasePacketPayload payload){channel(payload).sendToAll(payload);}
    public void sendToClientsTrackingAndSelf(Entity entity,BasePacketPayload payload){sendNear(entity,payload);}
    public void sendToClientsTrackingEntity(Entity entity,BasePacketPayload payload){sendNear(entity,payload);}
    private void sendNear(Entity entity,BasePacketPayload payload){if(!(entity.world instanceof WorldServer))return;WorldServer world=(WorldServer)entity.world;double radius=world.getMinecraftServer().getPlayerList().getViewDistance()*16D;sendToClientsAround(world,entity.getPositionVector(),radius,payload);}
    public void sendToClientsTrackingChunk(WorldServer world,ChunkPos chunk,BasePacketPayload payload){double radius=world.getMinecraftServer().getPlayerList().getViewDistance()*16D;sendToClientsAround(world,new Vec3d(chunk.x*16+8,128,chunk.z*16+8),radius,payload);}
    public void sendToClientsAround(WorldServer world,Vec3d pos,double radius,BasePacketPayload payload){channel(payload).sendToAllAround(payload,new NetworkRegistry.TargetPoint(world.provider.getDimension(),pos.x,pos.y,pos.z,radius));}
}
