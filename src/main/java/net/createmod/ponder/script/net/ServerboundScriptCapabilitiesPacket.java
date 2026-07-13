package net.createmod.ponder.script.net;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.CatnipPackets;
import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.ponder.script.ScriptSceneSnapshot;
import net.createmod.ponder.script.ScriptSceneSync;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.ByteBufUtils;

public final class ServerboundScriptCapabilitiesPacket implements ServerboundPacketPayload {
    private int protocol;
    private List<ResourceLocation> codecs = new ArrayList<ResourceLocation>();

    public ServerboundScriptCapabilitiesPacket() {
    }

    public ServerboundScriptCapabilitiesPacket(int protocol, List<ResourceLocation> codecs) {
        if (codecs == null || codecs.size() > ScriptSceneSnapshot.MAX_REQUIRED_CODECS)
            throw new IllegalArgumentException("Invalid Ponder script codec capability list");
        this.protocol = protocol;
        this.codecs = new ArrayList<ResourceLocation>(codecs);
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        protocol = buffer.readInt();
        int count = buffer.readUnsignedShort();
        if (count > ScriptSceneSnapshot.MAX_REQUIRED_CODECS)
            throw new IllegalArgumentException("Too many Ponder script codec capabilities");
        codecs = new ArrayList<ResourceLocation>(count);
        for (int i = 0; i < count; i++) {
            String value = ByteBufUtils.readUTF8String(buffer);
            if (value.length() > 256)
                throw new IllegalArgumentException("Ponder script codec id is too long");
            codecs.add(new ResourceLocation(value));
        }
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(protocol);
        buffer.writeShort(codecs.size());
        for (ResourceLocation codec : codecs)
            ByteBufUtils.writeUTF8String(buffer, codec.toString());
    }

    @Override
    public void handle(EntityPlayerMP player) {
        ScriptSceneSync.receiveCapabilities(player, protocol, codecs);
    }

    @Override
    public BasePacketPayload.PacketTypeProvider getTypeProvider() {
        return CatnipPackets.SERVERBOUND_SCRIPT_CAPABILITIES;
    }
}
