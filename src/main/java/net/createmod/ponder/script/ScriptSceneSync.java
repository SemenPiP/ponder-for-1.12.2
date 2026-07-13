package net.createmod.ponder.script;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.createmod.catnip.platform.CatnipServices;
import net.createmod.ponder.Ponder;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.script.net.ClientboundScriptSnapshotBeginPacket;
import net.createmod.ponder.script.net.ClientboundScriptSnapshotChunkPacket;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;

public final class ScriptSceneSync {
    public static final int CHUNK_BYTES = 256 * 1024;
    private static final AtomicInteger TRANSFER_IDS = new AtomicInteger();

    private ScriptSceneSync() {
    }

    public static void send(EntityPlayerMP player) {
        try {
            List<ScriptSceneDefinition> scenes = ScriptSceneRegistry.localSnapshot(false);
            ScriptSceneSnapshot.Encoded encoded = ScriptSceneSnapshot.encode(scenes);
            List<ResourceLocation> requiredCodecs = ScriptSceneSnapshot.requiredCodecs(scenes);
            int transfer = TRANSFER_IDS.incrementAndGet();
            int chunks = Math.max(1, (encoded.bytes.length + CHUNK_BYTES - 1) / CHUNK_BYTES);
            CatnipServices.NETWORK.sendToClient(player, new ClientboundScriptSnapshotBeginPacket(transfer,
                ScriptSceneSnapshot.PROTOCOL, chunks, encoded.bytes.length, encoded.uncompressedBytes,
                encoded.hash, requiredCodecs));
            for (int index = 0; index < chunks; index++) {
                int start = index * CHUNK_BYTES;
                int end = Math.min(encoded.bytes.length, start + CHUNK_BYTES);
                CatnipServices.NETWORK.sendToClient(player, new ClientboundScriptSnapshotChunkPacket(transfer, index,
                    Arrays.copyOfRange(encoded.bytes, start, end)));
            }
        } catch (IOException exception) {
            Ponder.LOGGER.error("Could not encode Ponder script scene snapshot for {}", player.getName(), exception);
        }
    }

    public static void sendAll(MinecraftServer server) {
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) send(player);
    }

    public static void clearServerScenes() {
        ScriptSceneRegistry.clearServerScenes();
        PonderIndex.reload();
    }
}
