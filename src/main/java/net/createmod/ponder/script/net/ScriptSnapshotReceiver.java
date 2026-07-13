package net.createmod.ponder.script.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import net.createmod.catnip.platform.CatnipServices;
import net.createmod.ponder.Ponder;
import net.createmod.ponder.api.script.ScriptInstructionCodecs;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.script.ScriptSceneDefinition;
import net.createmod.ponder.script.ScriptSceneRegistry;
import net.createmod.ponder.script.ScriptSceneSnapshot;
import net.createmod.ponder.script.ScriptSceneSync;
import net.minecraft.util.ResourceLocation;

public final class ScriptSnapshotReceiver {
    private static Transfer active;

    private ScriptSnapshotReceiver() {
    }

    public static synchronized void begin(int transferId, int protocol, int chunks, int compressedBytes,
                                          int uncompressedBytes, byte[] hash,
                                          List<ResourceLocation> requiredCodecs) {
        if (protocol != ScriptSceneSnapshot.PROTOCOL) {
            reject(transferId, "Unsupported protocol " + protocol + ", expected " + ScriptSceneSnapshot.PROTOCOL);
            active = null;
            return;
        }
        if (requiredCodecs == null || requiredCodecs.size() > ScriptSceneSnapshot.MAX_REQUIRED_CODECS) {
            reject(transferId, "Invalid required codec list");
            active = null;
            return;
        }
        for (ResourceLocation codec : requiredCodecs) {
            if (codec == null || ScriptInstructionCodecs.get(codec) == null) {
                reject(transferId, "Missing required codec " + codec);
                active = null;
                return;
            }
        }
        if (chunks < 1 || chunks > 64 || compressedBytes < 0
            || compressedBytes > ScriptSceneSnapshot.MAX_COMPRESSED_BYTES
            || uncompressedBytes < 0 || uncompressedBytes > ScriptSceneSnapshot.MAX_UNCOMPRESSED_BYTES
            || hash == null || hash.length != 32) {
            reject(transferId, "Invalid snapshot header");
            active = null;
            return;
        }
        active = new Transfer(transferId, chunks, compressedBytes, uncompressedBytes, hash);
    }

    public static synchronized void accept(int transferId, int index, byte[] bytes) {
        if (active == null || active.id != transferId || index < 0 || index >= active.parts.length
            || active.parts[index] != null || bytes == null || bytes.length > ScriptSceneSync.CHUNK_BYTES) {
            reject(transferId, "Invalid or duplicate snapshot chunk");
            active = null;
            return;
        }
        active.parts[index] = bytes.clone();
        active.received += bytes.length;
        if (!active.complete()) return;
        Transfer completed = active;
        active = null;
        try {
            byte[] compressed = completed.join();
            if (compressed.length != completed.compressedBytes
                || !Arrays.equals(ScriptSceneSnapshot.sha256(compressed), completed.hash))
                throw new IOException("Ponder script snapshot hash or length mismatch");
            List<ScriptSceneDefinition> definitions =
                ScriptSceneSnapshot.decode(compressed, completed.uncompressedBytes);
            ScriptSceneRegistry.replaceServerScenes(definitions);
            PonderIndex.reload();
            result(completed.id, true, "Applied " + definitions.size() + " scene(s)");
            Ponder.LOGGER.info("Applied {} server Ponder script scene(s)", definitions.size());
        } catch (IOException | RuntimeException exception) {
            reject(completed.id, exception.getMessage());
            Ponder.LOGGER.error("Rejected server Ponder script scene snapshot", exception);
        }
    }

    public static synchronized void reset() {
        active = null;
    }

    private static void reject(int transferId, String message) {
        result(transferId, false, message);
        Ponder.LOGGER.error("Rejected server Ponder script scene snapshot #{}: {}", transferId, message);
    }

    private static void result(int transferId, boolean accepted, String message) {
        try {
            CatnipServices.NETWORK.sendToServer(
                new ServerboundScriptSnapshotResultPacket(transferId, accepted, message));
        } catch (RuntimeException unavailableDuringTestsOrShutdown) {
            Ponder.LOGGER.debug("Could not send Ponder snapshot result packet", unavailableDuringTestsOrShutdown);
        }
    }

    private static final class Transfer {
        final int id;
        final byte[][] parts;
        final int compressedBytes;
        final int uncompressedBytes;
        final byte[] hash;
        int received;

        Transfer(int id, int chunks, int compressedBytes, int uncompressedBytes, byte[] hash) {
            this.id = id; this.parts = new byte[chunks][]; this.compressedBytes = compressedBytes;
            this.uncompressedBytes = uncompressedBytes; this.hash = hash.clone();
        }

        boolean complete() {
            if (received > compressedBytes) return true;
            for (byte[] part : parts) if (part == null) return false;
            return true;
        }

        byte[] join() throws IOException {
            ByteArrayOutputStream output = new ByteArrayOutputStream(compressedBytes);
            for (byte[] part : parts) output.write(part);
            return output.toByteArray();
        }
    }
}
