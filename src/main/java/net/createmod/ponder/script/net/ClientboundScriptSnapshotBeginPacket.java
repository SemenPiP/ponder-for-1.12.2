package net.createmod.ponder.script.net;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.CatnipPackets;
import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.createmod.ponder.api.script.ScriptInstructionCodecDescriptor;
import net.createmod.ponder.script.ScriptCodecDescriptors;
import net.createmod.ponder.script.ScriptSceneSnapshot;

public final class ClientboundScriptSnapshotBeginPacket implements ClientboundPacketPayload {
    private int transferId;
    private int protocol;
    private int chunks;
    private int compressedBytes;
    private int uncompressedBytes;
    private byte[] hash = new byte[32];
    private List<ScriptInstructionCodecDescriptor> requiredCodecs =
        new ArrayList<ScriptInstructionCodecDescriptor>();

    public ClientboundScriptSnapshotBeginPacket() {
    }

    public ClientboundScriptSnapshotBeginPacket(int transferId, int protocol, int chunks, int compressedBytes,
                                                int uncompressedBytes, byte[] hash,
                                                List<ScriptInstructionCodecDescriptor> requiredCodecs) {
        if (hash == null || hash.length != 32) throw new IllegalArgumentException("Snapshot hash must be SHA-256");
        if (requiredCodecs == null || requiredCodecs.size() > ScriptSceneSnapshot.MAX_REQUIRED_CODECS)
            throw new IllegalArgumentException("Invalid required codec list");
        this.transferId = transferId; this.protocol = protocol; this.chunks = chunks;
        this.compressedBytes = compressedBytes; this.uncompressedBytes = uncompressedBytes; this.hash = hash.clone();
        this.requiredCodecs = ScriptCodecDescriptors.validate(requiredCodecs);
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        transferId = buffer.readInt(); protocol = buffer.readInt(); chunks = buffer.readInt();
        compressedBytes = buffer.readInt();
        uncompressedBytes = buffer.readInt(); buffer.readBytes(hash);
        requiredCodecs = ScriptCodecDescriptors.read(buffer);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(transferId); buffer.writeInt(protocol); buffer.writeInt(chunks); buffer.writeInt(compressedBytes);
        buffer.writeInt(uncompressedBytes); buffer.writeBytes(hash);
        ScriptCodecDescriptors.write(buffer, requiredCodecs);
    }

    @Override public void handleClient() {
        ScriptSnapshotReceiver.begin(transferId, protocol, chunks, compressedBytes, uncompressedBytes, hash,
            requiredCodecs);
    }

    @Override public BasePacketPayload.PacketTypeProvider getTypeProvider() {
        return CatnipPackets.CLIENTBOUND_SCRIPT_SNAPSHOT_BEGIN;
    }
}
