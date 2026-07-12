package net.createmod.ponder.foundation.registration;

import java.util.Arrays;
import java.util.function.Function;

import net.createmod.ponder.api.registration.MultiTagBuilder;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.api.registration.TagBuilder;
import net.minecraft.util.ResourceLocation;

public final class GenericPonderTagRegistrationHelper<T> implements PonderTagRegistrationHelper<T> {
    private final PonderTagRegistrationHelper<ResourceLocation> delegate;
    private final Function<T, ResourceLocation> keyFunction;

    public GenericPonderTagRegistrationHelper(PonderTagRegistrationHelper<ResourceLocation> delegate,
                                              Function<T, ResourceLocation> keyFunction) {
        this.delegate = delegate;
        this.keyFunction = keyFunction;
    }

    @Override public <S> PonderTagRegistrationHelper<S> withKeyFunction(final Function<S, T> function) {
        return new GenericPonderTagRegistrationHelper<S>(delegate, function.andThen(keyFunction));
    }
    @Override public TagBuilder registerTag(ResourceLocation location) { return delegate.registerTag(location); }
    @Override public TagBuilder registerTag(String id) { return delegate.registerTag(id); }
    @Override public void addTagToComponent(T component, ResourceLocation tag) { delegate.addTagToComponent(keyFunction.apply(component), tag); }
    @Override public MultiTagBuilder.Tag<T> addToTag(ResourceLocation tag) { return new GenericMultiTagBuilder.TagImpl<T>(this, Arrays.asList(tag)); }
    @Override public MultiTagBuilder.Tag<T> addToTag(ResourceLocation... tags) { return new GenericMultiTagBuilder.TagImpl<T>(this, Arrays.asList(tags)); }
    @Override public MultiTagBuilder.Component addToComponent(T component) { return new GenericMultiTagBuilder.ComponentImpl<T>(this, Arrays.asList(component)); }
    @Override public MultiTagBuilder.Component addToComponent(T... components) { return new GenericMultiTagBuilder.ComponentImpl<T>(this, Arrays.asList(components)); }
}
