package net.createmod.catnip.net.packets;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.codecs.stream.StreamCodec;
import net.createmod.catnip.config.ConfigPath;
import net.createmod.catnip.config.ConfigRegistry;
import net.createmod.catnip.config.ConfigType;
import net.createmod.catnip.net.CatnipPackets;
import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.ClientboundPacketPayload;

public class ClientboundConfigPacket implements ClientboundPacketPayload {
    private static final StreamCodec<ByteBuf,String> PATH=CatnipStreamCodecBuilders.string(512),VALUE=CatnipStreamCodecBuilders.string(4096);
    private String path="",value="";
    public ClientboundConfigPacket(){}
    public ClientboundConfigPacket(String path,String value){this.path=path;this.value=value;}
    public void fromBytes(ByteBuf buffer){path=PATH.decode(buffer);value=VALUE.decode(buffer);}
    public void toBytes(ByteBuf buffer){PATH.encode(buffer,path);VALUE.encode(buffer,value);}
    public void handleClient(){ConfigPath parsed=ConfigPath.parse(path);if(parsed.getType()!=ConfigType.CLIENT)throw new IllegalArgumentException("Client packet may only change client config");ConfigRegistry.set(parsed,value);}
    public BasePacketPayload.PacketTypeProvider getTypeProvider(){return CatnipPackets.CLIENTBOUND_CONFIG;}
}
