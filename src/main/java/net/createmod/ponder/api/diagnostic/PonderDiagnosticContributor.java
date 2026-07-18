package net.createmod.ponder.api.diagnostic;

import net.minecraft.util.ResourceLocation;

public interface PonderDiagnosticContributor {
    ResourceLocation getId();

    void contribute(PonderDiagnosticContext context, PonderDiagnosticSink sink);
}
