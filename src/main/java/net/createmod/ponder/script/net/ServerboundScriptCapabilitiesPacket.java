package net.createmod.ponder.script.net;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.CatnipPackets;
import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.ponder.api.script.ScriptInstructionCodecDescriptor;
import net.createmod.ponder.script.ScriptCodecDescriptors;
import net.createmod.ponder.script.ScriptSceneSnapshot;
import net.createmod.ponder.script.ScriptSceneSync;
import net.minecraft.entity.player.EntityPlayerMP;

public final class ServerboundScriptCapabilitiesPacket implements ServerboundPacketPayload {
    private int protocol;
    private List<ScriptInstructionCodecDescriptor> codecs =
        new ArrayList<ScriptInstructionCodecDescriptor>();

    public ServerboundScriptCapabilitiesPacket() {
    }

    public ServerboundScriptCapabilitiesPacket(int protocol,
                                                List<ScriptInstructionCodecDescriptor> codecs) {
        if (codecs == null || codecs.size() > ScriptSceneSnapshot.MAX_REQUIRED_CODECS)
            throw new IllegalArgumentException("Invalid Ponder script codec capability list");
        this.protocol = protocol;
        this.codecs = ScriptCodecDescriptors.validate(codecs);
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        protocol = buffer.readInt();
        codecs = ScriptCodecDescriptors.read(buffer);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(protocol);
        ScriptCodecDescriptors.write(buffer, codecs);
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
