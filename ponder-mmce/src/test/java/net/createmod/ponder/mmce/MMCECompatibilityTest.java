package net.createmod.ponder.mmce;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MMCECompatibilityTest {
    @Test
    public void acceptsACompleteCompatibilityProbe() {
        assertTrue(MMCECompatibility.check(() -> {
        }, false));
    }

    @Test
    public void disablesCompatibilityWhenAnExpectedMethodIsMissing() {
        assertFalse(MMCECompatibility.check(() -> {
            throw new NoSuchMethodException("simulated future MMCE ABI");
        }, false));
    }

    @Test
    public void disablesCompatibilityOnLinkageFailure() {
        assertFalse(MMCECompatibility.check(() -> {
            throw new NoClassDefFoundError("simulated optional integration");
        }, false));
    }
}
