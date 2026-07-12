package net.createmod.ponder.foundation.registration;

import java.util.Arrays;
import java.util.function.Function;

import net.createmod.ponder.api.registration.MultiSceneBuilder;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.StoryBoardEntry;
import net.createmod.ponder.api.scene.PonderStoryBoard;
import net.minecraft.util.ResourceLocation;

public final class GenericPonderSceneRegistrationHelper<T> implements PonderSceneRegistrationHelper<T> {
    private final PonderSceneRegistrationHelper<ResourceLocation> delegate;
    private final Function<T, ResourceLocation> keyFunction;

    public GenericPonderSceneRegistrationHelper(PonderSceneRegistrationHelper<ResourceLocation> delegate,
                                                Function<T, ResourceLocation> keyFunction) {
        this.delegate = delegate;
        this.keyFunction = keyFunction;
    }

    @Override public <S> PonderSceneRegistrationHelper<S> withKeyFunction(final Function<S, T> function) {
        return new GenericPonderSceneRegistrationHelper<S>(delegate, function.andThen(keyFunction));
    }

    @Override public StoryBoardEntry addStoryBoard(T component, ResourceLocation schematicLocation,
                                                    PonderStoryBoard storyBoard, ResourceLocation... tags) {
        return delegate.addStoryBoard(keyFunction.apply(component), schematicLocation, storyBoard, tags);
    }

    @Override public StoryBoardEntry addStoryBoard(T component, String schematicPath,
                                                    PonderStoryBoard storyBoard, ResourceLocation... tags) {
        return delegate.addStoryBoard(keyFunction.apply(component), schematicPath, storyBoard, tags);
    }

    @Override public MultiSceneBuilder forComponents(T... components) {
        return new GenericMultiSceneBuilder<T>(this, Arrays.asList(components));
    }

    @Override public MultiSceneBuilder forComponents(Iterable<? extends T> components) {
        return new GenericMultiSceneBuilder<T>(this, components);
    }

    @Override public ResourceLocation asLocation(String path) { return delegate.asLocation(path); }
}
