package net.createmod.ponder.script.net;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.CatnipPackets;
import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.ponder.Ponder;
import net.createmod.ponder.script.ScriptSceneSnapshot;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;

public final class ServerboundScriptSnapshotResultPacket implements ServerboundPacketPayload {
    private int transferId;
    private int protocol;
    private boolean accepted;
    private String message = "";

    public ServerboundScriptSnapshotResultPacket() {
    }

    public ServerboundScriptSnapshotResultPacket(int transferId, boolean accepted, String message) {
        this.transferId = transferId;
        this.protocol = ScriptSceneSnapshot.PROTOCOL;
        this.accepted = accepted;
        this.message = sanitize(message);
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        transferId = buffer.readInt();
        protocol = buffer.readInt();
        accepted = buffer.readBoolean();
        message = sanitize(ByteBufUtils.readUTF8String(buffer));
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(transferId);
        buffer.writeInt(protocol);
        buffer.writeBoolean(accepted);
        ByteBufUtils.writeUTF8String(buffer, sanitize(message));
    }

    @Override
    public void handle(EntityPlayerMP player) {
        if (protocol != ScriptSceneSnapshot.PROTOCOL) {
            Ponder.LOGGER.warn("{} reported incompatible Ponder script protocol {}", player.getName(), protocol);
        } else if (accepted) {
            Ponder.LOGGER.info("{} applied Ponder script snapshot #{}", player.getName(), transferId);
        } else {
            Ponder.LOGGER.warn("{} rejected Ponder script snapshot #{}: {}", player.getName(), transferId, message);
        }
    }

    @Override
    public BasePacketPayload.PacketTypeProvider getTypeProvider() {
        return CatnipPackets.SERVERBOUND_SCRIPT_SNAPSHOT_RESULT;
    }

    private static String sanitize(String value) {
        if (value == null) return "";
        value = value.replace('\r', ' ').replace('\n', ' ');
        return value.length() > 1024 ? value.substring(0, 1024) : value;
    }
}
