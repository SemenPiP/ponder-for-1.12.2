package net.createmod.ponder.script.net;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.CatnipPackets;
import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.ClientboundPacketPayload;

public final class ClientboundScriptSnapshotCompletePacket implements ClientboundPacketPayload {
    private int transferId;

    public ClientboundScriptSnapshotCompletePacket() {
    }

    public ClientboundScriptSnapshotCompletePacket(int transferId) {
        this.transferId = transferId;
    }

    @Override public void fromBytes(ByteBuf buffer) { transferId = buffer.readInt(); }
    @Override public void toBytes(ByteBuf buffer) { buffer.writeInt(transferId); }
    @Override public void handleClient() { ScriptSnapshotReceiver.complete(transferId); }

    @Override
    public BasePacketPayload.PacketTypeProvider getTypeProvider() {
        return CatnipPackets.CLIENTBOUND_SCRIPT_SNAPSHOT_COMPLETE;
    }
}
