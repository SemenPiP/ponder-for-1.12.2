package net.createmod.ponder.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.math.MathHelper;

/** Development-only calculations mirroring the item renderer's final vertical translation. */
public final class PonderRenderHarnessProbe {
    private PonderRenderHarnessProbe() {
    }

    public static ItemRenderY sampleItemRenderY(EntityItem item, float partialTicks) {
        if (item == null)
            throw new IllegalArgumentException("An item entity is required");
        float partial = Math.max(0, Math.min(1, partialTicks));
        double interpolatedY = item.lastTickPosY + (item.posY - item.lastTickPosY) * partial;
        Render<?> renderer = Minecraft.getMinecraft().getRenderManager().getEntityRenderObject(item);
        boolean shouldBob = renderer instanceof RenderEntityItem && ((RenderEntityItem) renderer).shouldBob();
        double compensatedY = PonderWorldRenderer.compensateItemBob(interpolatedY, item.getAge(), partial,
            item.hoverStart, shouldBob);
        float phase = ((float) item.getAge() + partial) / 10.0F + item.hoverStart;
        double rendererBob = shouldBob ? MathHelper.sin(phase) * 0.1F + 0.1F : 0;
        return new ItemRenderY(interpolatedY, compensatedY, compensatedY + rendererBob, shouldBob);
    }

    public static final class ItemRenderY {
        public final double interpolatedY;
        public final double compensatedY;
        public final double effectiveY;
        public final boolean shouldBob;

        ItemRenderY(double interpolatedY, double compensatedY, double effectiveY, boolean shouldBob) {
            this.interpolatedY = interpolatedY;
            this.compensatedY = compensatedY;
            this.effectiveY = effectiveY;
            this.shouldBob = shouldBob;
        }

        @Override
        public String toString() {
            return "interpolated=" + interpolatedY + ", compensated=" + compensatedY
                + ", effective=" + effectiveY + ", shouldBob=" + shouldBob;
        }
    }
}
