package net.createmod.ponder.api.diagnostic;

import java.util.Collection;

import net.minecraft.util.ResourceLocation;

/**
 * Append-only output for addon diagnostics. Implementations namespace issue
 * codes and attribute dependencies to the active contributor.
 */
public interface PonderDiagnosticSink {
    void reportIssue(String localCode, PonderDiagnosticSeverity severity, String message,
                     ResourceLocation sceneId, int instructionIndex);

    default void reportIssue(String localCode, PonderDiagnosticSeverity severity, String message) {
        reportIssue(localCode, severity, message, null, -1);
    }

    void reportStructureDependency(ResourceLocation structureId, ResourceLocation providerId,
                                   String fingerprint, PonderStructureDependencyStatus status,
                                   Collection<ResourceLocation> sceneIds,
                                   Collection<ResourceLocation> components);
}
