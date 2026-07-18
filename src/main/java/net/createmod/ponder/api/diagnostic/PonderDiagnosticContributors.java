package net.createmod.ponder.api.diagnostic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import net.minecraft.util.ResourceLocation;

/** Registration and ServiceLoader discovery for append-only addon diagnostics. */
public final class PonderDiagnosticContributors {
    private static final Map<ResourceLocation, PonderDiagnosticContributor> CONTRIBUTORS =
        new LinkedHashMap<ResourceLocation, PonderDiagnosticContributor>();
    private static boolean discovered;

    private PonderDiagnosticContributors() {
    }

    public static synchronized void register(PonderDiagnosticContributor contributor) {
        if (contributor == null || contributor.getId() == null)
            throw new IllegalArgumentException("Ponder diagnostic contributor and id are required");
        ResourceLocation id = contributor.getId();
        if (id.toString().length() > 256)
            throw new IllegalArgumentException("Ponder diagnostic contributor id is too long: " + id);
        if (CONTRIBUTORS.containsKey(id))
            throw new IllegalArgumentException("Duplicate Ponder diagnostic contributor " + id);
        CONTRIBUTORS.put(id, contributor);
    }

    public static synchronized boolean unregister(ResourceLocation contributorId) {
        return contributorId != null && CONTRIBUTORS.remove(contributorId) != null;
    }

    public static synchronized void discover() {
        if (discovered)
            return;
        discovered = true;
        try {
            for (PonderDiagnosticContributor contributor : ServiceLoader.load(
                    PonderDiagnosticContributor.class,
                    PonderDiagnosticContributor.class.getClassLoader()))
                register(contributor);
        } catch (ServiceConfigurationError error) {
            throw new IllegalStateException("Could not discover Ponder diagnostic contributors", error);
        }
    }

    public static synchronized List<PonderDiagnosticContributor> snapshot() {
        return Collections.unmodifiableList(
            new ArrayList<PonderDiagnosticContributor>(CONTRIBUTORS.values()));
    }
}
