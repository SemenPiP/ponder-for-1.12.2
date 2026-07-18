package net.createmod.ponder.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.createmod.ponder.api.diagnostic.PonderSyncDiagnostic;
import net.minecraft.util.ResourceLocation;

final class ScriptSceneSyncTracker {
    static final String WAITING_CAPABILITIES = "waiting_capabilities";
    static final String SENDING = "sending";
    static final String WAITING_RESULT = "waiting_result";
    static final String ACCEPTED = "accepted";
    static final String REJECTED = "rejected";
    static final String TIMED_OUT = "timed_out";
    static final String TIMEOUT_MESSAGE = "Ponder script snapshot transfer timed out";

    private final Map<UUID, State> states = new LinkedHashMap<UUID, State>();

    void request(UUID playerId, String playerName, long now) {
        if (playerId == null) return;
        states.put(playerId, new State(playerName, now));
    }

    boolean contains(UUID playerId) {
        return states.containsKey(playerId);
    }

    boolean isWaitingForCapabilities(UUID playerId) {
        State state = states.get(playerId);
        return state != null && WAITING_CAPABILITIES.equals(state.status);
    }

    void remove(UUID playerId) {
        states.remove(playerId);
    }

    void recordCapabilities(UUID playerId, int protocol, List<ResourceLocation> codecs, long now) {
        State state = requireState(playerId);
        if (!WAITING_CAPABILITIES.equals(state.status))
            throw new IllegalStateException("Ponder script sync is not waiting for capabilities");
        state.protocol = protocol;
        state.codecs = sortedCopy(codecs);
        state.updatedAt = now;
    }

    void startTransfer(UUID playerId, int transferId, int compressedBytes, int uncompressedBytes, long now) {
        State state = requireState(playerId);
        state.transferId = transferId;
        state.startedAt = now;
        state.updatedAt = now;
        state.compressedBytes = compressedBytes;
        state.uncompressedBytes = uncompressedBytes;
        state.status = SENDING;
    }

    void reject(UUID playerId, String playerName, int protocol, List<ResourceLocation> codecs,
                int transferId, String message, long now) {
        State state = states.get(playerId);
        if (state == null) {
            state = new State(playerName, now);
            states.put(playerId, state);
        }
        state.protocol = protocol;
        state.codecs = sortedCopy(codecs);
        state.transferId = transferId;
        state.status = REJECTED;
        state.lastResult = message == null ? "" : message;
        state.updatedAt = now;
    }

    void markWaitingResult(UUID playerId, int transferId) {
        State state = states.get(playerId);
        if (state != null && state.transferId == transferId && SENDING.equals(state.status))
            state.status = WAITING_RESULT;
    }

    boolean recordResult(UUID playerId, int transferId, int protocol, int expectedProtocol,
                         boolean accepted, String message, long now) {
        State state = states.get(playerId);
        if (state == null || state.transferId != transferId || !WAITING_RESULT.equals(state.status))
            return false;
        if (protocol != expectedProtocol) {
            state.status = REJECTED;
            state.lastResult = "Protocol mismatch: " + protocol;
        } else {
            state.status = accepted ? ACCEPTED : REJECTED;
            state.lastResult = message == null ? "" : message;
        }
        state.updatedAt = now;
        return true;
    }

    List<Timeout> expire(long now, long timeoutMillis) {
        List<Timeout> expired = new ArrayList<Timeout>();
        for (Map.Entry<UUID, State> entry : states.entrySet()) {
            State state = entry.getValue();
            if (ACCEPTED.equals(state.status) || REJECTED.equals(state.status)
                || TIMED_OUT.equals(state.status) || now - state.startedAt <= timeoutMillis)
                continue;
            state.status = TIMED_OUT;
            state.lastResult = TIMEOUT_MESSAGE;
            state.updatedAt = now;
            expired.add(new Timeout(entry.getKey(), state.transferId, TIMEOUT_MESSAGE));
        }
        return expired;
    }

    List<PonderSyncDiagnostic> snapshotDiagnostics() {
        List<PonderSyncDiagnostic> result = new ArrayList<PonderSyncDiagnostic>();
        for (Map.Entry<UUID, State> entry : states.entrySet()) {
            State state = entry.getValue();
            result.add(new PonderSyncDiagnostic(entry.getKey(), state.playerName, state.protocol,
                state.codecs, state.transferId, state.status, state.startedAt, state.updatedAt,
                state.compressedBytes, state.uncompressedBytes, state.lastResult));
        }
        Collections.sort(result, new Comparator<PonderSyncDiagnostic>() {
            @Override
            public int compare(PonderSyncDiagnostic left, PonderSyncDiagnostic right) {
                return left.getPlayerName().compareToIgnoreCase(right.getPlayerName());
            }
        });
        return Collections.unmodifiableList(result);
    }

    private State requireState(UUID playerId) {
        State state = states.get(playerId);
        if (state == null)
            throw new IllegalStateException("No Ponder script sync state for " + playerId);
        return state;
    }

    private static List<ResourceLocation> sortedCopy(List<ResourceLocation> codecs) {
        if (codecs == null || codecs.isEmpty())
            return Collections.emptyList();
        List<ResourceLocation> copy = new ArrayList<ResourceLocation>(codecs);
        Collections.sort(copy, new Comparator<ResourceLocation>() {
            @Override
            public int compare(ResourceLocation left, ResourceLocation right) {
                return left.toString().compareTo(right.toString());
            }
        });
        return Collections.unmodifiableList(copy);
    }

    static final class Timeout {
        private final UUID playerId;
        private final int transferId;
        private final String message;

        Timeout(UUID playerId, int transferId, String message) {
            this.playerId = playerId;
            this.transferId = transferId;
            this.message = message;
        }

        UUID getPlayerId() {
            return playerId;
        }

        int getTransferId() {
            return transferId;
        }

        String getMessage() {
            return message;
        }
    }

    private static final class State {
        final String playerName;
        long startedAt;
        long updatedAt;
        int transferId;
        int protocol;
        int compressedBytes;
        int uncompressedBytes;
        List<ResourceLocation> codecs = Collections.emptyList();
        String status = WAITING_CAPABILITIES;
        String lastResult = "";

        State(String playerName, long now) {
            this.playerName = playerName == null ? "" : playerName;
            this.startedAt = now;
            this.updatedAt = now;
        }
    }
}
