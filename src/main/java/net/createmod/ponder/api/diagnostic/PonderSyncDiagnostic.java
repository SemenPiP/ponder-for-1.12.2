package net.createmod.ponder.api.diagnostic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import net.minecraft.util.ResourceLocation;

public final class PonderSyncDiagnostic {
    private final UUID playerId;
    private final String playerName;
    private final int protocol;
    private final List<ResourceLocation> codecs;
    private final int transferId;
    private final String status;
    private final long startedAt;
    private final long updatedAt;
    private final int compressedBytes;
    private final int uncompressedBytes;
    private final String lastResult;

    public PonderSyncDiagnostic(UUID playerId, String playerName, int protocol,
                                List<ResourceLocation> codecs, int transferId, String status,
                                long startedAt, long updatedAt, int compressedBytes,
                                int uncompressedBytes, String lastResult) {
        if (playerId == null)
            throw new IllegalArgumentException("Ponder sync player id is required");
        this.playerId = playerId;
        this.playerName = playerName == null ? "" : playerName;
        this.protocol = protocol;
        this.codecs = codecs == null || codecs.isEmpty() ? Collections.<ResourceLocation>emptyList()
            : Collections.unmodifiableList(new ArrayList<ResourceLocation>(codecs));
        this.transferId = transferId;
        this.status = status == null ? "" : status;
        this.startedAt = startedAt;
        this.updatedAt = updatedAt;
        this.compressedBytes = Math.max(0, compressedBytes);
        this.uncompressedBytes = Math.max(0, uncompressedBytes);
        this.lastResult = lastResult == null ? "" : lastResult;
    }

    public UUID getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public int getProtocol() { return protocol; }
    public List<ResourceLocation> getCodecs() { return codecs; }
    public int getTransferId() { return transferId; }
    public String getStatus() { return status; }
    public long getStartedAt() { return startedAt; }
    public long getUpdatedAt() { return updatedAt; }
    public int getCompressedBytes() { return compressedBytes; }
    public int getUncompressedBytes() { return uncompressedBytes; }
    public String getLastResult() { return lastResult; }
}
