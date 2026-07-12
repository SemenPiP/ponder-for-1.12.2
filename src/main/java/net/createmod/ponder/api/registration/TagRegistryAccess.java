package net.createmod.ponder.api.registration;

import java.util.List;
import java.util.Set;

import net.createmod.ponder.foundation.PonderTag;
import net.minecraft.util.ResourceLocation;

public interface TagRegistryAccess {
    PonderTag getRegisteredTag(ResourceLocation tagLocation);

    List<PonderTag> getListedTags();

    Set<PonderTag> getTags(ResourceLocation component);

    Set<ResourceLocation> getItems(ResourceLocation tag);

    Set<ResourceLocation> getItems(PonderTag tag);
}
