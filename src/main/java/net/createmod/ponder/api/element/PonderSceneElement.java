package net.createmod.ponder.api.element;

import net.createmod.ponder.foundation.PonderWorld;
import net.minecraft.util.BlockRenderLayer;

public interface PonderSceneElement extends PonderElement {
    default void renderFirst(PonderWorld world, float partialTicks) {
    }

    default void renderLayer(PonderWorld world, BlockRenderLayer layer, float partialTicks) {
    }

    default void renderLast(PonderWorld world, float partialTicks) {
    }
}
