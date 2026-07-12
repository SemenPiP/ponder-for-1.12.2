package net.createmod.catnip.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class GlStateGuardRestorationTest {
    @Test
    public void pairAttemptsCacheAndLaterStateAfterDriverFailure() {
        List<String> calls = new ArrayList<>();
        RuntimeException driverFailure = new RuntimeException("driver");
        GlStateGuard.Restoration restoration = new GlStateGuard.Restoration();

        restoration.pair(() -> {
            calls.add("driver");
            throw driverFailure;
        }, () -> calls.add("cache"));
        restoration.attempt(() -> calls.add("later"));

        try {
            restoration.finish();
            fail("Expected the first restoration failure");
        } catch (RuntimeException thrown) {
            assertSame(driverFailure, thrown);
        }
        assertEquals(Arrays.asList("driver", "cache", "later"), calls);
    }

    @Test
    public void finishKeepsFirstFailureAndSuppressesEveryLaterFailure() {
        RuntimeException first = new RuntimeException("first");
        IllegalStateException second = new IllegalStateException("second");
        AssertionError third = new AssertionError("third");
        GlStateGuard.Restoration restoration = new GlStateGuard.Restoration();

        restoration.attempt(() -> { throw first; });
        restoration.pair(() -> { throw second; }, () -> { throw third; });

        try {
            restoration.finish();
            fail("Expected the first restoration failure");
        } catch (RuntimeException thrown) {
            assertSame(first, thrown);
            assertEquals(2, thrown.getSuppressed().length);
            assertSame(second, thrown.getSuppressed()[0]);
            assertSame(third, thrown.getSuppressed()[1]);
        }
    }

    @Test
    public void finishPreservesAnErrorWhenItIsTheFirstFailure() {
        AssertionError first = new AssertionError("first");
        GlStateGuard.Restoration restoration = new GlStateGuard.Restoration();
        restoration.attempt(() -> { throw first; });

        try {
            restoration.finish();
            fail("Expected the original error");
        } catch (AssertionError thrown) {
            assertSame(first, thrown);
        }
    }

    @Test
    public void finishReturnsNormallyWhenEveryStepSucceeds() {
        GlStateGuard.Restoration restoration = new GlStateGuard.Restoration();
        restoration.pair(() -> { }, () -> { });
        restoration.finish();
    }
}
