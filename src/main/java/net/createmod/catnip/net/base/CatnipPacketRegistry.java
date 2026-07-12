package net.createmod.catnip.net.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.util.IThreadListener;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class CatnipPacketRegistry {
    public final String modId,networkVersion;
    private final Set<PacketType<?>> packets=new LinkedHashSet<PacketType<?>>();
    public final Set<PacketType<?>> packetsView=Collections.unmodifiableSet(packets);
    private final SimpleNetworkWrapper channel;
    private boolean registered;
    public CatnipPacketRegistry(String modId,int networkVersion){this(modId,String.valueOf(networkVersion));}
    public CatnipPacketRegistry(String modId,String networkVersion){if(modId==null||modId.isEmpty())throw new IllegalArgumentException("modId cannot be empty");this.modId=modId;this.networkVersion=networkVersion;channel=NetworkRegistry.INSTANCE.newSimpleChannel(modId);}
    public synchronized void registerPacket(PacketType<?> packet){if(registered)throw new IllegalStateException("Packets already registered");if(!packets.add(packet))throw new IllegalArgumentException("Duplicate packet class: "+packet.clazz);packet.owner=this;}
    public synchronized void registerAllPackets(){if(registered)throw new IllegalStateException("Packets already registered");CatnipServices.NETWORK.registerPackets(this);registered=true;}
    public synchronized void registerInternal(){int id=0;for(PacketType<?> packet:packets)registerOne(packet,id++);}
    @SuppressWarnings({"rawtypes","unchecked"}) private void registerOne(PacketType packet,int id){channel.registerMessage(new DispatchHandler(),packet.clazz,id,packet.side);}
    public SimpleNetworkWrapper getChannel(){return channel;}
    public static final class PacketType<T extends BasePacketPayload> {
        private final Class<T> clazz;private final Side side;private CatnipPacketRegistry owner;
        public PacketType(Class<T> clazz,Side side){this.clazz=clazz;this.side=side;}
        public Class<T> getPacketClass(){return clazz;} public Side getSide(){return side;} public CatnipPacketRegistry getOwner(){if(owner==null)throw new IllegalStateException("Packet type is not registered");return owner;}
    }
    private static final class DispatchHandler implements IMessageHandler<BasePacketPayload,IMessage> {
        public IMessage onMessage(final BasePacketPayload message,final MessageContext context){
            final IThreadListener thread=FMLCommonHandler.instance().getWorldThread(context.netHandler);
            thread.addScheduledTask(new Runnable(){public void run(){
                if(context.side==Side.SERVER&&message instanceof ServerboundPacketPayload)((ServerboundPacketPayload)message).handle(context.getServerHandler().player);
                else if(context.side==Side.CLIENT&&message instanceof ClientboundPacketPayload)((ClientboundPacketPayload)message).handleClient();
            }});return null;
        }
    }
}
