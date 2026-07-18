package net.createmod.ponder.api.diagnostic;

/** Read-only context supplied to addon diagnostic contributors. */
public final class PonderDiagnosticContext {
    private final PonderDiagnosticSnapshot snapshot;

    public PonderDiagnosticContext(PonderDiagnosticSnapshot snapshot) {
        if (snapshot == null)
            throw new IllegalArgumentException("Ponder diagnostic snapshot is required");
        this.snapshot = snapshot;
    }

    public PonderDiagnosticView getView() {
        return snapshot.getView();
    }

    public long getGeneration() {
        return snapshot.getGeneration();
    }

    public PonderDiagnosticSnapshot getSnapshot() {
        return snapshot;
    }
}
