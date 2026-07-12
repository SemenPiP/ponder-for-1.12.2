package net.createmod.catnip.net;

import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.CatnipPacketRegistry;
import net.createmod.catnip.net.packets.ClientboundConfigPacket;
import net.createmod.catnip.net.packets.ClientboundSimpleActionPacket;
import net.createmod.catnip.net.packets.ServerboundConfigPacket;
import net.minecraftforge.fml.relauncher.Side;

public enum CatnipPackets implements BasePacketPayload.PacketTypeProvider {
    SERVERBOUND_CONFIG(ServerboundConfigPacket.class,Side.SERVER),
    CLIENTBOUND_SIMPLE_ACTION(ClientboundSimpleActionPacket.class,Side.CLIENT),
    CLIENTBOUND_CONFIG(ClientboundConfigPacket.class,Side.CLIENT);
    private final CatnipPacketRegistry.PacketType<?> type;
    <T extends BasePacketPayload> CatnipPackets(Class<T> clazz,Side side){type=new CatnipPacketRegistry.PacketType<T>(clazz,side);}
    public CatnipPacketRegistry.PacketType<?> getPacketType(){return type;}
    private static CatnipPacketRegistry registry;
    public static synchronized CatnipPacketRegistry register(){if(registry!=null)return registry;registry=new CatnipPacketRegistry("ponder",1);for(CatnipPackets packet:values())registry.registerPacket(packet.type);registry.registerAllPackets();return registry;}
}
