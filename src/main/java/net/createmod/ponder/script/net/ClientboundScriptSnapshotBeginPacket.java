package net.createmod.ponder.script.net;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.CatnipPackets;
import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.createmod.ponder.script.ScriptSceneSnapshot;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.ByteBufUtils;

public final class ClientboundScriptSnapshotBeginPacket implements ClientboundPacketPayload {
    private int transferId;
    private int protocol;
    private int chunks;
    private int compressedBytes;
    private int uncompressedBytes;
    private byte[] hash = new byte[32];
    private List<ResourceLocation> requiredCodecs = new ArrayList<ResourceLocation>();

    public ClientboundScriptSnapshotBeginPacket() {
    }

    public ClientboundScriptSnapshotBeginPacket(int transferId, int protocol, int chunks, int compressedBytes,
                                                int uncompressedBytes, byte[] hash,
                                                List<ResourceLocation> requiredCodecs) {
        if (hash == null || hash.length != 32) throw new IllegalArgumentException("Snapshot hash must be SHA-256");
        if (requiredCodecs == null || requiredCodecs.size() > ScriptSceneSnapshot.MAX_REQUIRED_CODECS)
            throw new IllegalArgumentException("Invalid required codec list");
        this.transferId = transferId; this.protocol = protocol; this.chunks = chunks;
        this.compressedBytes = compressedBytes; this.uncompressedBytes = uncompressedBytes; this.hash = hash.clone();
        this.requiredCodecs = new ArrayList<ResourceLocation>(requiredCodecs);
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        transferId = buffer.readInt(); protocol = buffer.readInt(); chunks = buffer.readInt();
        compressedBytes = buffer.readInt();
        uncompressedBytes = buffer.readInt(); buffer.readBytes(hash);
        int codecCount = buffer.readUnsignedShort();
        if (codecCount > ScriptSceneSnapshot.MAX_REQUIRED_CODECS)
            throw new IllegalArgumentException("Too many required Ponder script codecs");
        requiredCodecs = new ArrayList<ResourceLocation>(codecCount);
        for (int i = 0; i < codecCount; i++) {
            String value = ByteBufUtils.readUTF8String(buffer);
            if (value.length() > 256) throw new IllegalArgumentException("Ponder script codec id is too long");
            requiredCodecs.add(new ResourceLocation(value));
        }
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(transferId); buffer.writeInt(protocol); buffer.writeInt(chunks); buffer.writeInt(compressedBytes);
        buffer.writeInt(uncompressedBytes); buffer.writeBytes(hash);
        buffer.writeShort(requiredCodecs.size());
        for (ResourceLocation codec : requiredCodecs) ByteBufUtils.writeUTF8String(buffer, codec.toString());
    }

    @Override public void handleClient() {
        ScriptSnapshotReceiver.begin(transferId, protocol, chunks, compressedBytes, uncompressedBytes, hash,
            requiredCodecs);
    }

    @Override public BasePacketPayload.PacketTypeProvider getTypeProvider() {
        return CatnipPackets.CLIENTBOUND_SCRIPT_SNAPSHOT_BEGIN;
    }
}
