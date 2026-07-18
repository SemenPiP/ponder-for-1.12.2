package net.createmod.ponder.foundation.diagnostic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.After;
import org.junit.Test;

import net.createmod.ponder.api.diagnostic.PonderDiagnosticContributor;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticContributors;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticContext;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticSeverity;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticSink;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticView;
import net.createmod.ponder.api.diagnostic.PonderStructureDependency;
import net.createmod.ponder.api.diagnostic.PonderStructureDependencyStatus;
import net.createmod.ponder.foundation.registration.PonderLocalization;
import net.createmod.ponder.foundation.registration.PonderSceneRegistry;
import net.minecraft.util.ResourceLocation;

public class PonderDiagnosticContributorTest {
    private static final ResourceLocation CONTRIBUTOR =
        new ResourceLocation("diagnostic_test", "contributor");
    private static final ResourceLocation FAILING =
        new ResourceLocation("diagnostic_test", "failing");

    @After
    public void removeContributors() {
        PonderDiagnosticContributors.unregister(CONTRIBUTOR);
        PonderDiagnosticContributors.unregister(FAILING);
        rebuild();
    }

    @Test
    public void contributorIssuesAreNamespacedAndDependenciesAreAttributed() {
        PonderDiagnosticContributors.register(new PonderDiagnosticContributor() {
            @Override public ResourceLocation getId() { return CONTRIBUTOR; }
            @Override
            public void contribute(PonderDiagnosticContext context, PonderDiagnosticSink sink) {
                sink.reportIssue("configuration", PonderDiagnosticSeverity.WARNING, "Check config");
                sink.reportStructureDependency(new ResourceLocation("diagnostic_test", "structure"),
                    new ResourceLocation("diagnostic_test", "provider"), "fingerprint",
                    PonderStructureDependencyStatus.AVAILABLE,
                    Collections.<ResourceLocation>emptyList(),
                    Collections.singletonList(new ResourceLocation("diagnostic_test", "component")));
            }
        });

        rebuild();

        assertTrue(PonderDiagnosticRegistry.snapshot(PonderDiagnosticView.EFFECTIVE).getIssues().stream()
            .anyMatch(issue -> "addon.diagnostic_test.contributor.configuration".equals(issue.getCode())));
        PonderStructureDependency dependency =
            PonderDiagnosticRegistry.structureDependencies(PonderDiagnosticView.EFFECTIVE).get(0);
        assertEquals(CONTRIBUTOR, dependency.getContributorId());
        assertEquals("fingerprint", dependency.getFingerprint());
    }

    @Test
    public void failingContributorDoesNotAbortDiagnosticGeneration() {
        PonderDiagnosticContributors.register(new PonderDiagnosticContributor() {
            @Override public ResourceLocation getId() { return FAILING; }
            @Override public void contribute(PonderDiagnosticContext context, PonderDiagnosticSink sink) {
                throw new IllegalStateException("broken");
            }
        });

        rebuild();

        assertTrue(PonderDiagnosticRegistry.snapshot(PonderDiagnosticView.EFFECTIVE).getIssues().stream()
            .anyMatch(issue -> "diagnostic.contributor_failed".equals(issue.getCode())));
    }

    private static void rebuild() {
        PonderSceneRegistry registry = new PonderSceneRegistry(new PonderLocalization());
        registry.freeze();
        PonderDiagnosticRegistry.rebuild(registry);
    }
}
