package net.createmod.catnip.net;

import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.CatnipPacketRegistry;
import net.createmod.catnip.net.packets.ClientboundConfigPacket;
import net.createmod.catnip.net.packets.ClientboundSimpleActionPacket;
import net.createmod.catnip.net.packets.ServerboundConfigPacket;
import net.createmod.ponder.Ponder;
import net.createmod.ponder.script.net.ClientboundScriptSnapshotBeginPacket;
import net.createmod.ponder.script.net.ClientboundScriptSnapshotChunkPacket;
import net.createmod.ponder.script.net.ClientboundScriptSnapshotCompletePacket;
import net.createmod.ponder.script.net.ClientboundScriptSnapshotStatusPacket;
import net.createmod.ponder.script.net.ServerboundScriptCapabilitiesPacket;
import net.createmod.ponder.script.net.ServerboundScriptSnapshotResultPacket;
import net.minecraftforge.fml.relauncher.Side;

public enum CatnipPackets implements BasePacketPayload.PacketTypeProvider {
    SERVERBOUND_CONFIG(ServerboundConfigPacket.class,Side.SERVER),
    CLIENTBOUND_SIMPLE_ACTION(ClientboundSimpleActionPacket.class,Side.CLIENT),
    CLIENTBOUND_CONFIG(ClientboundConfigPacket.class,Side.CLIENT),
    CLIENTBOUND_SCRIPT_SNAPSHOT_BEGIN(ClientboundScriptSnapshotBeginPacket.class,Side.CLIENT),
    CLIENTBOUND_SCRIPT_SNAPSHOT_CHUNK(ClientboundScriptSnapshotChunkPacket.class,Side.CLIENT),
    SERVERBOUND_SCRIPT_SNAPSHOT_RESULT(ServerboundScriptSnapshotResultPacket.class,Side.SERVER),
    SERVERBOUND_SCRIPT_CAPABILITIES(ServerboundScriptCapabilitiesPacket.class,Side.SERVER),
    CLIENTBOUND_SCRIPT_SNAPSHOT_COMPLETE(ClientboundScriptSnapshotCompletePacket.class,Side.CLIENT),
    CLIENTBOUND_SCRIPT_SNAPSHOT_STATUS(ClientboundScriptSnapshotStatusPacket.class,Side.CLIENT);
    private final CatnipPacketRegistry.PacketType<?> type;
    <T extends BasePacketPayload> CatnipPackets(Class<T> clazz,Side side){type=new CatnipPacketRegistry.PacketType<T>(clazz,side);}
    public CatnipPacketRegistry.PacketType<?> getPacketType(){return type;}
    private static CatnipPacketRegistry registry;
    public static synchronized CatnipPacketRegistry register(){if(registry!=null)return registry;registry=new CatnipPacketRegistry(Ponder.MOD_ID,3);for(CatnipPackets packet:values())registry.registerPacket(packet.type);registry.registerAllPackets();return registry;}
}
