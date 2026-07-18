package net.createmod.ponder.script;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import net.createmod.catnip.platform.CatnipServices;
import net.createmod.ponder.Ponder;
import net.createmod.ponder.api.diagnostic.PonderSyncDiagnostic;
import net.createmod.ponder.api.script.ScriptInstructionCodecDescriptor;
import net.createmod.ponder.script.net.ClientboundScriptSnapshotBeginPacket;
import net.createmod.ponder.script.net.ClientboundScriptSnapshotChunkPacket;
import net.createmod.ponder.script.net.ClientboundScriptSnapshotCompletePacket;
import net.createmod.ponder.script.net.ClientboundScriptSnapshotStatusPacket;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

public final class ScriptSceneSync {
    public static final int CHUNK_BYTES = 256 * 1024;
    public static final long TRANSFER_TIMEOUT_MILLIS = 30_000L;
    private static final AtomicInteger TRANSFER_IDS = new AtomicInteger();
    private static final ScriptSceneSyncTracker CLIENTS = new ScriptSceneSyncTracker();

    private ScriptSceneSync() {
    }

    public static void send(EntityPlayerMP player) {
        requestCapabilities(player);
    }

    public static synchronized void requestCapabilities(EntityPlayerMP player) {
        if (player == null) return;
        CLIENTS.request(player.getUniqueID(), player.getName(), System.currentTimeMillis());
        CatnipServices.NETWORK.sendToClient(player, new ClientboundScriptSnapshotStatusPacket(0,
            ClientboundScriptSnapshotStatusPacket.REQUEST_CAPABILITIES,
            "Ponder script capabilities requested"));
    }

    public static synchronized void receiveCapabilities(EntityPlayerMP player, int protocol,
                                                        List<ScriptInstructionCodecDescriptor> codecs) {
        if (player == null) return;
        if (!CLIENTS.contains(player.getUniqueID())) {
            rejectAndTrack(player, protocol, codecs, 0,
                "Unexpected Ponder script capability response");
            return;
        }
        if (!CLIENTS.isWaitingForCapabilities(player.getUniqueID())) {
            reject(player, 0, "Stale Ponder script capability response");
            return;
        }
        if (protocol != ScriptSceneSnapshot.PROTOCOL) {
            rejectAndTrack(player, protocol, codecs, 0,
                "Unsupported Ponder script protocol " + protocol);
            return;
        }
        if (codecs == null || codecs.size() > ScriptSceneSnapshot.MAX_REQUIRED_CODECS) {
            rejectAndTrack(player, protocol, codecs, 0,
                "Invalid Ponder script codec capability list");
            return;
        }
        Map<net.minecraft.util.ResourceLocation, ScriptInstructionCodecDescriptor> supported;
        try {
            supported = ScriptCodecDescriptors.byId(codecs);
        } catch (RuntimeException invalid) {
            rejectAndTrack(player, protocol, codecs, 0,
                "Invalid Ponder script codec capability list: " + invalid.getMessage());
            return;
        }
        long now = System.currentTimeMillis();
        CLIENTS.recordCapabilities(player.getUniqueID(), protocol,
            new ArrayList<ScriptInstructionCodecDescriptor>(supported.values()), now);
        try {
            List<ScriptSceneDefinition> scenes = ScriptSceneRegistry.localSnapshot(false);
            ScriptSceneSnapshot.Encoded encoded = ScriptSceneSnapshot.encode(scenes);
            List<ScriptInstructionCodecDescriptor> requiredCodecs = encoded.requirements;
            for (ScriptInstructionCodecDescriptor required : requiredCodecs) {
                ScriptInstructionCodecDescriptor capability = supported.get(required.getId());
                if (capability == null) {
                    rejectAndTrack(player, protocol, codecs, 0,
                        "Missing required Ponder script codec " + required.getId());
                    return;
                }
                if (capability.getProtocolVersion() != required.getProtocolVersion()) {
                    rejectAndTrack(player, protocol, codecs, 0,
                        "Ponder script codec version mismatch for " + required.getId()
                            + ": client " + capability.getProtocolVersion() + ", server "
                            + required.getProtocolVersion());
                    return;
                }
                if (!capability.satisfies(required)) {
                    rejectAndTrack(player, protocol, codecs, 0,
                        "Missing required capabilities for Ponder script codec " + required.getId()
                            + ": " + missingCapabilities(capability, required));
                    return;
                }
            }
            int transfer = nextTransferId();
            int chunks = Math.max(1, (encoded.bytes.length + CHUNK_BYTES - 1) / CHUNK_BYTES);
            CLIENTS.startTransfer(player.getUniqueID(), transfer, encoded.bytes.length, encoded.uncompressedBytes,
                requiredCodecs, System.currentTimeMillis());
            CatnipServices.NETWORK.sendToClient(player, new ClientboundScriptSnapshotBeginPacket(transfer,
                ScriptSceneSnapshot.PROTOCOL, chunks, encoded.bytes.length, encoded.uncompressedBytes,
                encoded.hash, requiredCodecs));
            for (int index = 0; index < chunks; index++) {
                int start = index * CHUNK_BYTES;
                int end = Math.min(encoded.bytes.length, start + CHUNK_BYTES);
                CatnipServices.NETWORK.sendToClient(player, new ClientboundScriptSnapshotChunkPacket(transfer, index,
                    Arrays.copyOfRange(encoded.bytes, start, end)));
            }
            CatnipServices.NETWORK.sendToClient(player, new ClientboundScriptSnapshotCompletePacket(transfer));
            CLIENTS.markWaitingResult(player.getUniqueID(), transfer);
        } catch (IOException exception) {
            Ponder.LOGGER.error("Could not encode Ponder script scene snapshot for {}", player.getName(), exception);
            rejectAndTrack(player, protocol, codecs, 0,
                "Could not encode Ponder script scene snapshot");
        }
    }

    public static synchronized void receiveResult(EntityPlayerMP player, int transferId, int protocol,
                                                  boolean accepted, String message) {
        if (player == null) return;
        if (!CLIENTS.recordResult(player.getUniqueID(), transferId, protocol, ScriptSceneSnapshot.PROTOCOL,
            accepted, message, System.currentTimeMillis())) {
            Ponder.LOGGER.warn("{} reported stale Ponder script snapshot result #{}", player.getName(), transferId);
        }
    }

    public static void sendAll(MinecraftServer server) {
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) requestCapabilities(player);
    }

    public static synchronized void tick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        for (ScriptSceneSyncTracker.Timeout timeout : CLIENTS.expire(now, TRANSFER_TIMEOUT_MILLIS)) {
            EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(timeout.getPlayerId());
            if (player != null)
                reject(player, timeout.getTransferId(), timeout.getMessage());
        }
    }

    public static synchronized void logout(EntityPlayerMP player) {
        if (player != null) CLIENTS.remove(player.getUniqueID());
    }

    public static void clearServerScenes() {
        ScriptSceneRegistry.clearServerScenesAndReload();
    }

    public static synchronized List<PonderSyncDiagnostic> snapshotDiagnostics() {
        return CLIENTS.snapshotDiagnostics();
    }

    private static int nextTransferId() {
        return TRANSFER_IDS.updateAndGet(value -> value == Integer.MAX_VALUE ? 1 : value + 1);
    }

    private static void reject(EntityPlayerMP player, int transferId, String message) {
        CatnipServices.NETWORK.sendToClient(player, new ClientboundScriptSnapshotStatusPacket(transferId,
            ClientboundScriptSnapshotStatusPacket.REJECTED, message));
        Ponder.LOGGER.warn("Rejected Ponder script sync for {}: {}", player.getName(), message);
    }

    private static void rejectAndTrack(EntityPlayerMP player, int protocol,
                                       List<ScriptInstructionCodecDescriptor> codecs,
                                       int transferId, String message) {
        CLIENTS.reject(player.getUniqueID(), player.getName(), protocol, codecs, transferId, message,
            System.currentTimeMillis());
        reject(player, transferId, message);
    }

    private static List<net.minecraft.util.ResourceLocation> missingCapabilities(
            ScriptInstructionCodecDescriptor capability,
            ScriptInstructionCodecDescriptor requirement) {
        List<net.minecraft.util.ResourceLocation> missing =
            new ArrayList<net.minecraft.util.ResourceLocation>(requirement.getCapabilities());
        missing.removeAll(capability.getCapabilities());
        return missing;
    }
}
