package net.createmod.ponder.api.diagnostic;

public enum PonderDiagnosticView {
    LOCAL,
    SERVER,
    EFFECTIVE;

    public static PonderDiagnosticView parse(String value) {
        if (value == null || value.trim().isEmpty())
            return EFFECTIVE;
        for (PonderDiagnosticView view : values())
            if (view.name().equalsIgnoreCase(value.trim()))
                return view;
        throw new IllegalArgumentException("Unknown Ponder diagnostic view: " + value);
    }
}
