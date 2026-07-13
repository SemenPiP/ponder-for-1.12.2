package net.createmod.ponder.script.net;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.CatnipPackets;
import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraftforge.fml.common.network.ByteBufUtils;

public final class ClientboundScriptSnapshotStatusPacket implements ClientboundPacketPayload {
    public static final String REQUEST_CAPABILITIES = "request_capabilities";
    public static final String REJECTED = "rejected";

    private int transferId;
    private String status = "";
    private String message = "";

    public ClientboundScriptSnapshotStatusPacket() {
    }

    public ClientboundScriptSnapshotStatusPacket(int transferId, String status, String message) {
        this.transferId = transferId;
        this.status = sanitize(status, 64);
        this.message = sanitize(message, 1024);
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        transferId = buffer.readInt();
        status = sanitize(ByteBufUtils.readUTF8String(buffer), 64);
        message = sanitize(ByteBufUtils.readUTF8String(buffer), 1024);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(transferId);
        ByteBufUtils.writeUTF8String(buffer, sanitize(status, 64));
        ByteBufUtils.writeUTF8String(buffer, sanitize(message, 1024));
    }

    @Override
    public void handleClient() {
        ScriptSnapshotReceiver.status(transferId, status, message);
    }

    @Override
    public BasePacketPayload.PacketTypeProvider getTypeProvider() {
        return CatnipPackets.CLIENTBOUND_SCRIPT_SNAPSHOT_STATUS;
    }

    private static String sanitize(String value, int limit) {
        if (value == null) return "";
        value = value.replace('\r', ' ').replace('\n', ' ');
        return value.length() > limit ? value.substring(0, limit) : value;
    }
}
