package net.createmod.catnip.net.packets;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.codecs.stream.StreamCodec;
import net.createmod.catnip.config.ConfigPath;
import net.createmod.catnip.config.ConfigRegistry;
import net.createmod.catnip.config.ConfigType;
import net.createmod.catnip.net.CatnipPackets;
import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.entity.player.EntityPlayerMP;

public class ServerboundConfigPacket implements ServerboundPacketPayload {
    private static final StreamCodec<ByteBuf,String> MOD=CatnipStreamCodecBuilders.string(64),PATH=CatnipStreamCodecBuilders.string(512),VALUE=CatnipStreamCodecBuilders.string(4096);
    private String modID="",path="",value="";
    public ServerboundConfigPacket(){}
    public ServerboundConfigPacket(String modID,String path,Object value){this(modID,path,serialize(value));}
    public ServerboundConfigPacket(String modID,String path,String value){this.modID=modID;this.path=path;this.value=value;}
    public void fromBytes(ByteBuf buffer){modID=MOD.decode(buffer);path=PATH.decode(buffer);value=VALUE.decode(buffer);}
    public void toBytes(ByteBuf buffer){MOD.encode(buffer,modID);PATH.encode(buffer,path);VALUE.encode(buffer,value);}
    public void handle(EntityPlayerMP player){if(!player.canUseCommand(2,"catnip"))return;ConfigPath parsed=ConfigPath.parse(path);if(!parsed.getModId().equals(modID)||parsed.getType()==ConfigType.CLIENT)return;ConfigRegistry.set(parsed,value);}
    public BasePacketPayload.PacketTypeProvider getTypeProvider(){return CatnipPackets.SERVERBOUND_CONFIG;}
    public static String serialize(Object value){if(value==null)throw new IllegalArgumentException("Cannot serialize null");if(value instanceof Enum)return((Enum<?>)value).name();if(value instanceof Boolean||value instanceof Number||value instanceof String)return String.valueOf(value);throw new IllegalArgumentException("Unsupported config type: "+value.getClass());}
    @SuppressWarnings({"rawtypes","unchecked"}) public static Object deserialize(Object type,String value){if(type instanceof Boolean){if(!"true".equalsIgnoreCase(value)&&!"false".equalsIgnoreCase(value))throw new IllegalArgumentException("Expected boolean");return Boolean.parseBoolean(value);}if(type instanceof Enum)return Enum.valueOf(((Enum)type).getDeclaringClass(),value);if(type instanceof Integer)return Integer.parseInt(value);if(type instanceof Float)return Float.parseFloat(value);if(type instanceof Double)return Double.parseDouble(value);if(type instanceof Long)return Long.parseLong(value);if(type instanceof String)return value;throw new IllegalArgumentException("Unsupported config type: "+type.getClass());}
}
