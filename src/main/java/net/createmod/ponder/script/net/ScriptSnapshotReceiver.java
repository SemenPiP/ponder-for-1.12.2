package net.createmod.ponder.script.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.createmod.catnip.platform.CatnipServices;
import net.createmod.ponder.Ponder;
import net.createmod.ponder.api.script.ScriptInstructionCodecDescriptor;
import net.createmod.ponder.api.script.ScriptInstructionCodecs;
import net.createmod.ponder.script.ScriptCodecDescriptors;
import net.createmod.ponder.script.ScriptSceneDefinition;
import net.createmod.ponder.script.ScriptSceneRegistry;
import net.createmod.ponder.script.ScriptSceneSnapshot;
import net.createmod.ponder.script.ScriptSceneSync;
import net.createmod.ponder.script.ScriptSyncNotices;
import net.minecraft.util.ResourceLocation;

public final class ScriptSnapshotReceiver {
    private static Transfer active;

    private ScriptSnapshotReceiver() {
    }

    public static synchronized void begin(int transferId, int protocol, int chunks, int compressedBytes,
                                          int uncompressedBytes, byte[] hash,
                                          List<ScriptInstructionCodecDescriptor> requiredCodecs) {
        if (protocol != ScriptSceneSnapshot.PROTOCOL) {
            rejectBegin(transferId, "Unsupported protocol " + protocol + ", expected "
                + ScriptSceneSnapshot.PROTOCOL);
            return;
        }
        if (requiredCodecs == null || requiredCodecs.size() > ScriptSceneSnapshot.MAX_REQUIRED_CODECS) {
            rejectBegin(transferId, "Invalid required codec list");
            return;
        }
        List<ScriptInstructionCodecDescriptor> requirements;
        try {
            requirements = ScriptCodecDescriptors.validate(requiredCodecs);
        } catch (RuntimeException invalid) {
            rejectBegin(transferId, "Invalid required codec list: " + invalid.getMessage());
            return;
        }
        for (ScriptInstructionCodecDescriptor required : requirements) {
            ScriptInstructionCodecDescriptor capability =
                ScriptInstructionCodecs.getDescriptor(required.getId());
            if (capability == null) {
                rejectBegin(transferId, "Missing required codec " + required.getId());
                return;
            }
            if (capability.getProtocolVersion() != required.getProtocolVersion()) {
                rejectBegin(transferId, "Codec version mismatch for " + required.getId());
                return;
            }
            if (!capability.satisfies(required)) {
                rejectBegin(transferId, "Missing required codec capabilities for " + required.getId());
                return;
            }
        }
        if (chunks < 1 || chunks > 64 || compressedBytes < 0
            || compressedBytes > ScriptSceneSnapshot.MAX_COMPRESSED_BYTES
            || uncompressedBytes < 0 || uncompressedBytes > ScriptSceneSnapshot.MAX_UNCOMPRESSED_BYTES
            || hash == null || hash.length != 32) {
            rejectBegin(transferId, "Invalid snapshot header");
            return;
        }
        if (active != null && active.id != transferId)
            result(active.id, false, "Superseded by Ponder script snapshot #" + transferId);
        active = new Transfer(transferId, chunks, compressedBytes, uncompressedBytes, hash, requirements,
            System.currentTimeMillis());
    }

    public static synchronized void accept(int transferId, int index, byte[] bytes) {
        if (active == null || active.id != transferId) {
            Ponder.LOGGER.debug("Ignoring stale Ponder script snapshot chunk #{} for transfer {}",
                index, transferId);
            return;
        }
        if (index < 0 || index >= active.parts.length || active.parts[index] != null
            || bytes == null || bytes.length > ScriptSceneSync.CHUNK_BYTES
            || active.received + bytes.length > active.compressedBytes) {
            reject(transferId, "Invalid or duplicate snapshot chunk");
            active = null;
            return;
        }
        active.parts[index] = bytes.clone();
        active.received += bytes.length;
    }

    public static synchronized void complete(int transferId) {
        if (active == null || active.id != transferId) {
            Ponder.LOGGER.debug("Ignoring stale Ponder script snapshot completion for transfer {}", transferId);
            return;
        }
        if (!active.hasAllParts()) {
            reject(transferId, "Ponder script snapshot completed with missing chunks");
            active = null;
            return;
        }
        Transfer completed = active;
        active = null;
        try {
            byte[] compressed = completed.join();
            if (compressed.length != completed.compressedBytes
                || !Arrays.equals(ScriptSceneSnapshot.sha256(compressed), completed.hash))
                throw new IOException("Ponder script snapshot hash or length mismatch");
            ScriptSceneSnapshot.Decoded decoded =
                ScriptSceneSnapshot.decodeContent(compressed, completed.uncompressedBytes);
            if (!completed.requirements.equals(decoded.requirements))
                throw new IOException("Snapshot Begin codec requirements do not match snapshot body");
            ScriptSceneRegistry.replaceServerScenesAndReload(decoded.scenes);
            result(completed.id, true, "Applied " + decoded.scenes.size() + " scene(s)");
            Ponder.LOGGER.info("Applied {} server Ponder script scene(s)", decoded.scenes.size());
        } catch (IOException | RuntimeException exception) {
            reject(completed.id, exception.getMessage());
            Ponder.LOGGER.error("Rejected server Ponder script scene snapshot", exception);
        }
    }

    public static synchronized void status(int transferId, String status, String message) {
        if (ClientboundScriptSnapshotStatusPacket.REQUEST_CAPABILITIES.equals(status)) {
            if (active != null) {
                result(active.id, false, "Superseded by a new Ponder script capability request");
                active = null;
            }
            CatnipServices.NETWORK.sendToServer(new ServerboundScriptCapabilitiesPacket(
                ScriptSceneSnapshot.PROTOCOL, ScriptCodecDescriptors.localCapabilities()));
            return;
        }
        if (ClientboundScriptSnapshotStatusPacket.REJECTED.equals(status)) {
            if (active != null && (transferId == 0 || active.id == transferId))
                active = null;
            String notice = "Ponder server scenes were not applied"
                + (message == null || message.isEmpty() ? "." : ": " + message);
            ScriptSyncNotices.record(notice);
            Ponder.LOGGER.warn(notice);
        }
    }

    public static synchronized void tick() {
        if (active == null) return;
        if (System.currentTimeMillis() - active.startedAt <= ScriptSceneSync.TRANSFER_TIMEOUT_MILLIS) return;
        int transferId = active.id;
        active = null;
        reject(transferId, "Ponder script snapshot transfer timed out");
        ScriptSyncNotices.record("Ponder server scene transfer timed out; local scenes remain available.");
    }

    public static synchronized void reset() {
        active = null;
    }

    private static void reject(int transferId, String message) {
        result(transferId, false, message);
        Ponder.LOGGER.error("Rejected server Ponder script scene snapshot #{}: {}", transferId, message);
    }

    private static void rejectBegin(int transferId, String message) {
        reject(transferId, message);
        if (active != null && active.id == transferId)
            active = null;
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
        final List<ScriptInstructionCodecDescriptor> requirements;
        final long startedAt;
        int received;

        Transfer(int id, int chunks, int compressedBytes, int uncompressedBytes, byte[] hash,
                 List<ScriptInstructionCodecDescriptor> requirements, long startedAt) {
            this.id = id; this.parts = new byte[chunks][]; this.compressedBytes = compressedBytes;
            this.uncompressedBytes = uncompressedBytes; this.hash = hash.clone();
            this.requirements = requirements;
            this.startedAt = startedAt;
        }

        boolean hasAllParts() {
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
