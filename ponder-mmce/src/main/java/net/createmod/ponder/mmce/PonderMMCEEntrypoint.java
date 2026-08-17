package net.createmod.ponder.mmce;

import net.createmod.ponder.api.structure.PonderStructureProviders;
import net.createmod.ponder.api.subject.PonderSubjectResolvers;
import net.createmod.ponder.mmce.structure.MMCEStructureProvider;
import net.createmod.ponder.mmce.subject.MMCEBlueprintResolver;

/**
 * Lifecycle bridge shared by the integrated Ponder jar and the legacy addon.
 * Its public signature intentionally contains no MMCE or Forge event types.
 */
public final class PonderMMCEEntrypoint {
    private static boolean compatibilityEnabled;

    private PonderMMCEEntrypoint() {
    }

    public static synchronized void preInit() {
        if (compatibilityEnabled || !MMCECompatibility.isSupported()) return;
        PonderStructureProviders.register(MMCEStructureProvider.INSTANCE);
        PonderSubjectResolvers.register(PonderMMCE.BLUEPRINT_RESOLVER_ID,
            MMCEBlueprintResolver.PRIORITY, MMCEBlueprintResolver.INSTANCE);
        compatibilityEnabled = true;
    }

    public static synchronized void postInit() {
        if (compatibilityEnabled) MMCEStructureProvider.INSTANCE.invalidate();
    }
}
