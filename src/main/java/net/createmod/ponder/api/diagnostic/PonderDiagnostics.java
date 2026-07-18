package net.createmod.ponder.api.diagnostic;

import java.util.List;

import net.createmod.ponder.foundation.PonderIndex;

public final class PonderDiagnostics {
    private PonderDiagnostics() {
    }

    public static PonderDiagnosticSnapshot snapshot(PonderDiagnosticView view) {
        return PonderIndex.getDiagnosticSnapshot(view == null ? PonderDiagnosticView.EFFECTIVE : view);
    }

    public static List<PonderSyncDiagnostic> syncStatuses() {
        return PonderIndex.getSyncDiagnostics();
    }
}
