package net.createmod.ponder.api.diagnostic;

import net.minecraft.util.ResourceLocation;

public final class PonderDiagnosticIssue {
    private final String code;
    private final PonderDiagnosticSeverity severity;
    private final String message;
    private final ResourceLocation sceneId;
    private final int instructionIndex;

    public PonderDiagnosticIssue(String code, PonderDiagnosticSeverity severity, String message,
                                 ResourceLocation sceneId, int instructionIndex) {
        if (code == null || !code.matches("[a-z0-9_.-]{1,96}"))
            throw new IllegalArgumentException("Invalid Ponder diagnostic code: " + code);
        if (severity == null)
            throw new IllegalArgumentException("Ponder diagnostic severity is required");
        if (message == null || message.trim().isEmpty())
            throw new IllegalArgumentException("Ponder diagnostic message is required");
        this.code = code;
        this.severity = severity;
        this.message = message;
        this.sceneId = sceneId;
        this.instructionIndex = instructionIndex;
    }

    public PonderDiagnosticIssue(String code, PonderDiagnosticSeverity severity, String message) {
        this(code, severity, message, null, -1);
    }

    public String getCode() {
        return code;
    }

    public PonderDiagnosticSeverity getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    public ResourceLocation getSceneId() {
        return sceneId;
    }

    public int getInstructionIndex() {
        return instructionIndex;
    }

    public boolean hasInstructionIndex() {
        return instructionIndex >= 0;
    }
}
