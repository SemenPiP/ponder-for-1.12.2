package net.createmod.catnip.net.packets;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.codecs.stream.StreamCodec;
import net.createmod.catnip.net.CatnipPackets;
import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.ClientboundPacketPayload;

public class ClientboundSimpleActionPacket implements ClientboundPacketPayload {
    private static final StreamCodec<ByteBuf,String> ACTION=CatnipStreamCodecBuilders.string(64),VALUE=CatnipStreamCodecBuilders.string(4096);
    private static final Map<String,Supplier<Consumer<String>>> ACTIONS=new ConcurrentHashMap<String,Supplier<Consumer<String>>>();
    private String action="",value="";
    public ClientboundSimpleActionPacket(){}
    public ClientboundSimpleActionPacket(String action,String value){validateAction(action);this.action=action;this.value=value==null?"":value;if(this.value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>4096)throw new IllegalArgumentException("Action value too long");}
    public static void addAction(String name,Supplier<Consumer<String>> action){validateAction(name);if(action==null)throw new NullPointerException("action");ACTIONS.put(name,action);}
    public static void removeAction(String name){ACTIONS.remove(name);}
    public void fromBytes(ByteBuf buffer){action=ACTION.decode(buffer);value=VALUE.decode(buffer);}
    public void toBytes(ByteBuf buffer){ACTION.encode(buffer,action);VALUE.encode(buffer,value);}
    public void handleClient(){Supplier<Consumer<String>> actionHandler=ACTIONS.get(action);if(actionHandler!=null)actionHandler.get().accept(value);}
    public BasePacketPayload.PacketTypeProvider getTypeProvider(){return CatnipPackets.CLIENTBOUND_SIMPLE_ACTION;}
    public String getAction(){return action;} public String getValue(){return value;}
    private static void validateAction(String name){if(name==null||!name.matches("[A-Za-z0-9_.-]{1,64}"))throw new IllegalArgumentException("Invalid action name: "+name);}
}
