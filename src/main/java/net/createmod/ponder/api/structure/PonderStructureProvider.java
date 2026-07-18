package net.createmod.ponder.api.structure;

import java.io.IOException;

import net.minecraft.util.ResourceLocation;

/**
 * Supplies raw Ponder structure data before resource-pack and jar lookup.
 *
 * <p>Higher priorities run first. Providers with the same priority retain
 * their registration order.</p>
 */
public interface PonderStructureProvider {
    ResourceLocation getId();

    default int getPriority() {
        return 0;
    }

    PonderStructureProviderResult find(ResourceLocation structureId) throws IOException;

    default void invalidate() {
    }
}
