package net.createmod.ponder.render;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import net.minecraft.util.math.MathHelper;

public class PonderWorldRendererItemTest {
    @Test
    public void bobCompensationKeepsTheVanillaAverageHeight() {
        int[] ages = {0, 1, 17, 80, 5999};
        float[] partialTicks = {0, .25F, .5F, 1};
        float[] hoverStarts = {0, .75F, (float) Math.PI, (float) (Math.PI * 2)};
        double baseY = 12.25;

        for (int age : ages) {
            for (float partialTick : partialTicks) {
                for (float hoverStart : hoverStarts) {
                    double compensated = PonderWorldRenderer.compensateItemBob(
                        baseY, age, partialTick, hoverStart, true);
                    float phase = ((float) age + partialTick) / 10.0F + hoverStart;
                    double vanillaBob = MathHelper.sin(phase) * 0.1F + 0.1F;
                    assertEquals(baseY + 0.1, compensated + vanillaBob, 1e-7);
                }
            }
        }
    }

    @Test
    public void disabledBobCompensationLeavesTheRenderCoordinateUntouched() {
        assertEquals(4.75, PonderWorldRenderer.compensateItemBob(
            4.75, 42, .5F, 1.25F, false), 0);
    }
}
