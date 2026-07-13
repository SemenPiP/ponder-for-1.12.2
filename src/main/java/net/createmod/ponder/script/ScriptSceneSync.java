package net.createmod.ponder.script;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import net.createmod.catnip.platform.CatnipServices;
import net.createmod.ponder.Ponder;
import net.createmod.ponder.script.net.ClientboundScriptSnapshotBeginPacket;
import net.createmod.ponder.script.net.ClientboundScriptSnapshotChunkPacket;
import net.createmod.ponder.script.net.ClientboundScriptSnapshotCompletePacket;
import net.createmod.ponder.script.net.ClientboundScriptSnapshotStatusPacket;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;

public final class ScriptSceneSync {
    public static final int CHUNK_BYTES = 256 * 1024;
    public static final long TRANSFER_TIMEOUT_MILLIS = 30_000L;
    private static final AtomicInteger TRANSFER_IDS = new AtomicInteger();
    private static final Map<UUID, ClientState> CLIENTS = new LinkedHashMap<UUID, ClientState>();

    private ScriptSceneSync() {
    }

    public static void send(EntityPlayerMP player) {
        requestCapabilities(player);
    }

    public static synchronized void requestCapabilities(EntityPlayerMP player) {
        if (player == null) return;
        CLIENTS.put(player.getUniqueID(), new ClientState(System.currentTimeMillis(), "waiting_capabilities"));
        CatnipServices.NETWORK.sendToClient(player, new ClientboundScriptSnapshotStatusPacket(0,
            ClientboundScriptSnapshotStatusPacket.REQUEST_CAPABILITIES,
            "Ponder script capabilities requested"));
    }

    public static synchronized void receiveCapabilities(EntityPlayerMP player, int protocol,
                                                        List<ResourceLocation> codecs) {
        if (player == null) return;
        ClientState state = CLIENTS.get(player.getUniqueID());
        if (state == null) {
            reject(player, 0, "Unexpected Ponder script capability response");
            return;
        }
        if (protocol != ScriptSceneSnapshot.PROTOCOL) {
            reject(player, 0, "Unsupported Ponder script protocol " + protocol);
            CLIENTS.remove(player.getUniqueID());
            return;
        }
        if (codecs == null || codecs.size() > ScriptSceneSnapshot.MAX_REQUIRED_CODECS) {
            reject(player, 0, "Invalid Ponder script codec capability list");
            CLIENTS.remove(player.getUniqueID());
            return;
        }
        Set<ResourceLocation> supported = new HashSet<ResourceLocation>();
        for (ResourceLocation codec : codecs) {
            if (codec == null || !supported.add(codec)) {
                reject(player, 0, "Invalid or duplicate Ponder script codec capability");
                CLIENTS.remove(player.getUniqueID());
                return;
            }
        }
        try {
            List<ScriptSceneDefinition> scenes = ScriptSceneRegistry.localSnapshot(false);
            ScriptSceneSnapshot.Encoded encoded = ScriptSceneSnapshot.encode(scenes);
            List<ResourceLocation> requiredCodecs = ScriptSceneSnapshot.requiredCodecs(scenes);
            for (ResourceLocation required : requiredCodecs) {
                if (!supported.contains(required)) {
                    reject(player, 0, "Missing required Ponder script codec " + required);
                    CLIENTS.remove(player.getUniqueID());
                    return;
                }
            }
            int transfer = nextTransferId();
            int chunks = Math.max(1, (encoded.bytes.length + CHUNK_BYTES - 1) / CHUNK_BYTES);
            state.transferId = transfer;
            state.startedAt = System.currentTimeMillis();
            state.status = "sending";
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
            state.status = "waiting_result";
        } catch (IOException exception) {
            Ponder.LOGGER.error("Could not encode Ponder script scene snapshot for {}", player.getName(), exception);
            reject(player, 0, "Could not encode Ponder script scene snapshot");
            CLIENTS.remove(player.getUniqueID());
        }
    }

    public static synchronized void receiveResult(EntityPlayerMP player, int transferId, int protocol,
                                                  boolean accepted, String message) {
        if (player == null) return;
        ClientState state = CLIENTS.get(player.getUniqueID());
        if (state == null || state.transferId != transferId) {
            Ponder.LOGGER.warn("{} reported stale Ponder script snapshot result #{}", player.getName(), transferId);
            return;
        }
        if (protocol != ScriptSceneSnapshot.PROTOCOL) {
            state.status = "rejected";
            state.lastResult = "Protocol mismatch: " + protocol;
        } else {
            state.status = accepted ? "accepted" : "rejected";
            state.lastResult = message == null ? "" : message;
        }
        state.startedAt = System.currentTimeMillis();
    }

    public static void sendAll(MinecraftServer server) {
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) requestCapabilities(player);
    }

    public static synchronized void tick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, ClientState>> iterator = CLIENTS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ClientState> entry = iterator.next();
            ClientState state = entry.getValue();
            if ("accepted".equals(state.status) || "rejected".equals(state.status)) continue;
            if (now - state.startedAt <= TRANSFER_TIMEOUT_MILLIS) continue;
            EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(entry.getKey());
            if (player != null)
                reject(player, state.transferId, "Ponder script snapshot transfer timed out");
            iterator.remove();
        }
    }

    public static synchronized void logout(EntityPlayerMP player) {
        if (player != null) CLIENTS.remove(player.getUniqueID());
    }

    public static void clearServerScenes() {
        ScriptSceneRegistry.clearServerScenesAndReload();
    }

    private static int nextTransferId() {
        return TRANSFER_IDS.updateAndGet(value -> value == Integer.MAX_VALUE ? 1 : value + 1);
    }

    private static void reject(EntityPlayerMP player, int transferId, String message) {
        CatnipServices.NETWORK.sendToClient(player, new ClientboundScriptSnapshotStatusPacket(transferId,
            ClientboundScriptSnapshotStatusPacket.REJECTED, message));
        Ponder.LOGGER.warn("Rejected Ponder script sync for {}: {}", player.getName(), message);
    }

    private static final class ClientState {
        long startedAt;
        int transferId;
        String status;
        String lastResult = "";

        ClientState(long startedAt, String status) {
            this.startedAt = startedAt;
            this.status = status;
        }
    }
}
