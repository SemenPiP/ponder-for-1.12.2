package net.createmod.ponder.foundation.registration;

import java.util.Arrays;
import java.util.function.Function;

import net.createmod.ponder.api.registration.MultiTagBuilder;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.api.registration.TagBuilder;
import net.createmod.ponder.foundation.PonderTag;
import net.minecraft.util.ResourceLocation;

public final class DefaultPonderTagRegistrationHelper implements PonderTagRegistrationHelper<ResourceLocation> {
    private final String namespace;
    private final PonderTagRegistry registry;
    private final PonderLocalization localization;

    public DefaultPonderTagRegistrationHelper(String namespace, PonderTagRegistry registry,
                                               PonderLocalization localization) {
        this.namespace = namespace;
        this.registry = registry;
        this.localization = localization;
    }

    @Override
    public <S> PonderTagRegistrationHelper<S> withKeyFunction(Function<S, ResourceLocation> keyGen) {
        return new GenericPonderTagRegistrationHelper<S>(this, keyGen);
    }

    @Override public TagBuilder registerTag(ResourceLocation location) { return new PonderTagBuilder(location, this::finish); }
    @Override public TagBuilder registerTag(String id) { return registerTag(new ResourceLocation(namespace, id)); }

    private void finish(PonderTagBuilder builder) {
        localization.registerTag(builder.id, builder.title, builder.description);
        PonderTag tag = new PonderTag(builder.id, builder.textureIconLocation, builder.itemIcon, builder.mainItem);
        registry.registerTag(tag);
        if (builder.addToIndex) registry.listTag(tag);
    }

    @Override public void addTagToComponent(ResourceLocation component, ResourceLocation tag) {
        registry.addTagToComponent(tag, component);
    }

    @Override public MultiTagBuilder.Tag<ResourceLocation> addToTag(ResourceLocation tag) {
        return new GenericMultiTagBuilder.TagImpl<ResourceLocation>(this, Arrays.asList(tag));
    }

    @Override public MultiTagBuilder.Tag<ResourceLocation> addToTag(ResourceLocation... tags) {
        return new GenericMultiTagBuilder.TagImpl<ResourceLocation>(this, Arrays.asList(tags));
    }

    @Override public MultiTagBuilder.Component addToComponent(ResourceLocation component) {
        return new GenericMultiTagBuilder.ComponentImpl<ResourceLocation>(this, Arrays.asList(component));
    }

    @Override public MultiTagBuilder.Component addToComponent(ResourceLocation... components) {
        return new GenericMultiTagBuilder.ComponentImpl<ResourceLocation>(this, Arrays.asList(components));
    }
}
