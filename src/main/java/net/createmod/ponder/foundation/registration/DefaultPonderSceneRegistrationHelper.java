package net.createmod.ponder.foundation.registration;

import java.util.Arrays;
import java.util.function.Function;

import net.createmod.ponder.api.registration.MultiSceneBuilder;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.StoryBoardEntry;
import net.createmod.ponder.api.scene.PonderStoryBoard;
import net.createmod.ponder.foundation.PonderStoryBoardEntry;
import net.minecraft.util.ResourceLocation;

public final class DefaultPonderSceneRegistrationHelper implements PonderSceneRegistrationHelper<ResourceLocation> {
    private final String namespace;
    private final String pluginClass;
    private final PonderSceneRegistry registry;

    public DefaultPonderSceneRegistrationHelper(String namespace, PonderSceneRegistry registry) {
        this(namespace, "", registry);
    }

    public DefaultPonderSceneRegistrationHelper(String namespace, String pluginClass,
                                                 PonderSceneRegistry registry) {
        if (namespace == null || namespace.trim().isEmpty())
            throw new IllegalArgumentException("Plugin namespace may not be blank");
        this.namespace = namespace;
        this.pluginClass = pluginClass == null ? "" : pluginClass;
        this.registry = registry;
    }

    @Override
    public <S> PonderSceneRegistrationHelper<S> withKeyFunction(Function<S, ResourceLocation> keyGen) {
        return new GenericPonderSceneRegistrationHelper<S>(this, keyGen);
    }

    @Override
    public StoryBoardEntry addStoryBoard(ResourceLocation component, ResourceLocation schematicLocation,
                                         PonderStoryBoard storyBoard, ResourceLocation... tags) {
        PonderStoryBoardEntry entry = new PonderStoryBoardEntry(storyBoard, namespace, schematicLocation, component);
        entry.setPluginClass(pluginClass);
        entry.highlightTags(tags);
        registry.addStoryBoard(entry);
        return entry;
    }

    @Override
    public StoryBoardEntry addStoryBoard(ResourceLocation component, String schematicPath,
                                         PonderStoryBoard storyBoard, ResourceLocation... tags) {
        return addStoryBoard(component, asLocation(schematicPath), storyBoard, tags);
    }

    @Override public MultiSceneBuilder forComponents(ResourceLocation... components) {
        return new GenericMultiSceneBuilder<ResourceLocation>(this, Arrays.asList(components));
    }

    @Override public MultiSceneBuilder forComponents(Iterable<? extends ResourceLocation> components) {
        return new GenericMultiSceneBuilder<ResourceLocation>(this, components);
    }

    @Override public ResourceLocation asLocation(String path) { return new ResourceLocation(namespace, path); }
}
