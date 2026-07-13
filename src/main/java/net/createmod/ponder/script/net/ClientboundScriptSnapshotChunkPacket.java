package net.createmod.ponder.script.net;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.CatnipPackets;
import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.createmod.ponder.script.ScriptSceneSync;

public final class ClientboundScriptSnapshotChunkPacket implements ClientboundPacketPayload {
    private int transferId;
    private int index;
    private byte[] bytes = new byte[0];

    public ClientboundScriptSnapshotChunkPacket() {
    }

    public ClientboundScriptSnapshotChunkPacket(int transferId, int index, byte[] bytes) {
        if (bytes.length > ScriptSceneSync.CHUNK_BYTES) throw new IllegalArgumentException("Script chunk too large");
        this.transferId = transferId; this.index = index; this.bytes = bytes.clone();
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        transferId = buffer.readInt(); index = buffer.readInt();
        int length = buffer.readInt();
        if (length < 0 || length > ScriptSceneSync.CHUNK_BYTES || length > buffer.readableBytes())
            throw new IllegalArgumentException("Invalid Ponder script chunk length " + length);
        bytes = new byte[length]; buffer.readBytes(bytes);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(transferId); buffer.writeInt(index); buffer.writeInt(bytes.length); buffer.writeBytes(bytes);
    }

    @Override public void handleClient() {
        ScriptSnapshotReceiver.accept(transferId, index, bytes);
    }

    @Override public BasePacketPayload.PacketTypeProvider getTypeProvider() {
        return CatnipPackets.CLIENTBOUND_SCRIPT_SNAPSHOT_CHUNK;
    }
}
