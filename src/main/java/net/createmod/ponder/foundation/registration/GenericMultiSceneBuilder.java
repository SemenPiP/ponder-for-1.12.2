package net.createmod.ponder.foundation.registration;

import java.util.function.Consumer;

import net.createmod.ponder.api.registration.MultiSceneBuilder;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.StoryBoardEntry;
import net.createmod.ponder.api.scene.PonderStoryBoard;
import net.minecraft.util.ResourceLocation;

public final class GenericMultiSceneBuilder<T> implements MultiSceneBuilder {
    private final PonderSceneRegistrationHelper<T> helper;
    private final Iterable<? extends T> components;

    public GenericMultiSceneBuilder(PonderSceneRegistrationHelper<T> helper, Iterable<? extends T> components) {
        this.helper = helper;
        this.components = components;
    }

    @Override public MultiSceneBuilder addStoryBoard(ResourceLocation location, PonderStoryBoard board) {
        return addStoryBoard(location, board, new Consumer<StoryBoardEntry>() {
            @Override public void accept(StoryBoardEntry ignored) { }
        });
    }

    @Override public MultiSceneBuilder addStoryBoard(ResourceLocation location, PonderStoryBoard board,
                                                      final ResourceLocation... tags) {
        return addStoryBoard(location, board, new Consumer<StoryBoardEntry>() {
            @Override public void accept(StoryBoardEntry entry) { entry.highlightTags(tags); }
        });
    }

    @Override public MultiSceneBuilder addStoryBoard(ResourceLocation location, PonderStoryBoard board,
                                                      Consumer<StoryBoardEntry> extras) {
        for (T component : components)
            extras.accept(helper.addStoryBoard(component, location, board));
        return this;
    }

    @Override public MultiSceneBuilder addStoryBoard(String path, PonderStoryBoard board) {
        return addStoryBoard(helper.asLocation(path), board);
    }

    @Override public MultiSceneBuilder addStoryBoard(String path, PonderStoryBoard board, ResourceLocation... tags) {
        return addStoryBoard(helper.asLocation(path), board, tags);
    }

    @Override public MultiSceneBuilder addStoryBoard(String path, PonderStoryBoard board,
                                                      Consumer<StoryBoardEntry> extras) {
        return addStoryBoard(helper.asLocation(path), board, extras);
    }
}
