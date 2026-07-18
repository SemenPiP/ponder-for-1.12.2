package net.createmod.ponder.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import net.createmod.ponder.api.diagnostic.PonderSyncDiagnostic;
import net.createmod.ponder.api.script.ScriptInstructionCodecDescriptor;
import net.minecraft.util.ResourceLocation;

public class ScriptSceneSyncTrackerTest {
    private static final int PROTOCOL = ScriptSceneSnapshot.PROTOCOL;
    private static final UUID PLAYER_ID = UUID.fromString("00000000-0000-0000-0000-000000000017");

    @Test
    public void requestStartsWaitingForCapabilities() {
        ScriptSceneSyncTracker tracker = new ScriptSceneSyncTracker();
        tracker.request(PLAYER_ID, "Alice", 100L);

        PonderSyncDiagnostic diagnostic = onlyDiagnostic(tracker);
        assertEquals("Alice", diagnostic.getPlayerName());
        assertEquals(ScriptSceneSyncTracker.WAITING_CAPABILITIES, diagnostic.getStatus());
        assertEquals(100L, diagnostic.getStartedAt());
        assertEquals(100L, diagnostic.getUpdatedAt());
    }

    @Test
    public void capabilitiesAndTransferMoveThroughSendingToWaitingResult() {
        ScriptSceneSyncTracker tracker = new ScriptSceneSyncTracker();
        tracker.request(PLAYER_ID, "Alice", 100L);
        tracker.recordCapabilities(PLAYER_ID, PROTOCOL, Arrays.asList(
            descriptor("z", 1), descriptor("a", 2)), 110L);
        tracker.startTransfer(PLAYER_ID, 7, 20, 80,
            Collections.singletonList(descriptor("a", 2)), 120L);

        PonderSyncDiagnostic sending = onlyDiagnostic(tracker);
        assertEquals(ScriptSceneSyncTracker.SENDING, sending.getStatus());
        assertEquals(PROTOCOL, sending.getProtocol());
        assertEquals(Arrays.asList(new ResourceLocation("a", "codec"),
            new ResourceLocation("z", "codec")), sending.getCodecs());
        assertEquals(2, sending.getCodecDescriptors().size());
        assertEquals(1, sending.getRequiredCodecDescriptors().size());
        assertEquals(7, sending.getTransferId());
        assertEquals(20, sending.getCompressedBytes());
        assertEquals(80, sending.getUncompressedBytes());
        assertEquals(120L, sending.getStartedAt());
        assertEquals(120L, sending.getUpdatedAt());

        tracker.markWaitingResult(PLAYER_ID, 7);
        assertEquals(ScriptSceneSyncTracker.WAITING_RESULT, onlyDiagnostic(tracker).getStatus());
    }

    @Test
    public void acceptsAndRejectsCurrentResults() {
        ScriptSceneSyncTracker tracker = new ScriptSceneSyncTracker();
        tracker.request(PLAYER_ID, "Alice", 100L);
        tracker.startTransfer(PLAYER_ID, 7, 20, 80, Collections.emptyList(), 120L);
        tracker.markWaitingResult(PLAYER_ID, 7);

        assertTrue(tracker.recordResult(PLAYER_ID, 7, PROTOCOL, PROTOCOL, true, "Applied", 130L));
        PonderSyncDiagnostic accepted = onlyDiagnostic(tracker);
        assertEquals(ScriptSceneSyncTracker.ACCEPTED, accepted.getStatus());
        assertEquals("Applied", accepted.getLastResult());
        assertEquals(120L, accepted.getStartedAt());
        assertEquals(130L, accepted.getUpdatedAt());

        tracker.request(PLAYER_ID, "Alice", 200L);
        tracker.startTransfer(PLAYER_ID, 8, 21, 81, Collections.emptyList(), 220L);
        tracker.markWaitingResult(PLAYER_ID, 8);
        assertTrue(tracker.recordResult(PLAYER_ID, 8, PROTOCOL, PROTOCOL, false, "Rejected", 230L));
        assertEquals(ScriptSceneSyncTracker.REJECTED, onlyDiagnostic(tracker).getStatus());
        assertEquals("Rejected", onlyDiagnostic(tracker).getLastResult());
    }

    @Test
    public void capabilityRejectionIsRetainedForDiagnostics() {
        ScriptSceneSyncTracker tracker = new ScriptSceneSyncTracker();
        tracker.request(PLAYER_ID, "Alice", 100L);
        tracker.reject(PLAYER_ID, "Alice", 99,
            Arrays.asList(descriptor("test", 1)), 0, "Protocol mismatch", 110L);

        PonderSyncDiagnostic diagnostic = onlyDiagnostic(tracker);
        assertEquals(ScriptSceneSyncTracker.REJECTED, diagnostic.getStatus());
        assertEquals(99, diagnostic.getProtocol());
        assertEquals("Protocol mismatch", diagnostic.getLastResult());
        assertEquals(100L, diagnostic.getStartedAt());
        assertEquals(110L, diagnostic.getUpdatedAt());
    }

    @Test
    public void timeoutIsRetainedAndOnlyReportedOnce() {
        ScriptSceneSyncTracker tracker = new ScriptSceneSyncTracker();
        tracker.request(PLAYER_ID, "Alice", 100L);
        tracker.startTransfer(PLAYER_ID, 7, 20, 80, Collections.emptyList(), 120L);
        tracker.markWaitingResult(PLAYER_ID, 7);

        List<ScriptSceneSyncTracker.Timeout> expired = tracker.expire(30_121L, 30_000L);
        assertEquals(1, expired.size());
        assertEquals(PLAYER_ID, expired.get(0).getPlayerId());
        assertEquals(7, expired.get(0).getTransferId());
        assertEquals(ScriptSceneSyncTracker.TIMEOUT_MESSAGE, expired.get(0).getMessage());
        assertEquals(ScriptSceneSyncTracker.TIMED_OUT, onlyDiagnostic(tracker).getStatus());
        assertEquals(ScriptSceneSyncTracker.TIMEOUT_MESSAGE, onlyDiagnostic(tracker).getLastResult());
        assertTrue(tracker.expire(60_122L, 30_000L).isEmpty());
    }

    @Test
    public void reentryReplacesStateAndOldTransferCannotOverwriteIt() {
        ScriptSceneSyncTracker tracker = new ScriptSceneSyncTracker();
        tracker.request(PLAYER_ID, "Alice", 100L);
        tracker.startTransfer(PLAYER_ID, 7, 20, 80, Collections.emptyList(), 120L);
        tracker.markWaitingResult(PLAYER_ID, 7);

        tracker.request(PLAYER_ID, "Alice", 200L);

        assertFalse(tracker.recordResult(PLAYER_ID, 7, PROTOCOL, PROTOCOL, true, "Old", 210L));
        PonderSyncDiagnostic diagnostic = onlyDiagnostic(tracker);
        assertEquals(ScriptSceneSyncTracker.WAITING_CAPABILITIES, diagnostic.getStatus());
        assertEquals(0, diagnostic.getTransferId());
        assertEquals(200L, diagnostic.getStartedAt());
    }

    @Test
    public void staleResultDoesNotOverwriteCurrentTransfer() {
        ScriptSceneSyncTracker tracker = new ScriptSceneSyncTracker();
        tracker.request(PLAYER_ID, "Alice", 100L);
        tracker.startTransfer(PLAYER_ID, 9, 20, 80, Collections.emptyList(), 120L);
        tracker.markWaitingResult(PLAYER_ID, 9);

        assertFalse(tracker.recordResult(PLAYER_ID, 8, PROTOCOL, PROTOCOL, false, "Stale", 130L));
        PonderSyncDiagnostic diagnostic = onlyDiagnostic(tracker);
        assertEquals(ScriptSceneSyncTracker.WAITING_RESULT, diagnostic.getStatus());
        assertEquals(9, diagnostic.getTransferId());
        assertEquals("", diagnostic.getLastResult());
    }

    @Test
    public void terminalStateDoesNotAcceptLateCapabilities() {
        ScriptSceneSyncTracker tracker = new ScriptSceneSyncTracker();
        tracker.request(PLAYER_ID, "Alice", 100L);
        tracker.reject(PLAYER_ID, "Alice", 99,
            Collections.<ScriptInstructionCodecDescriptor>emptyList(),
            0, "Rejected", 110L);

        assertFalse(tracker.isWaitingForCapabilities(PLAYER_ID));
        try {
            tracker.recordCapabilities(PLAYER_ID, PROTOCOL,
                Collections.<ScriptInstructionCodecDescriptor>emptyList(), 120L);
            throw new AssertionError("Terminal sync state accepted late capabilities");
        } catch (IllegalStateException expected) {
        }
        assertEquals(ScriptSceneSyncTracker.REJECTED, onlyDiagnostic(tracker).getStatus());
    }

    @Test
    public void logoutRemovesState() {
        ScriptSceneSyncTracker tracker = new ScriptSceneSyncTracker();
        tracker.request(PLAYER_ID, "Alice", 100L);

        tracker.remove(PLAYER_ID);

        assertFalse(tracker.contains(PLAYER_ID));
        assertTrue(tracker.snapshotDiagnostics().isEmpty());
    }

    private static PonderSyncDiagnostic onlyDiagnostic(ScriptSceneSyncTracker tracker) {
        List<PonderSyncDiagnostic> diagnostics = tracker.snapshotDiagnostics();
        assertEquals(1, diagnostics.size());
        return diagnostics.get(0);
    }

    private static ScriptInstructionCodecDescriptor descriptor(String namespace, int version) {
        return new ScriptInstructionCodecDescriptor(new ResourceLocation(namespace, "codec"), version,
            Collections.singleton(new ResourceLocation(namespace, "base")));
    }
}
