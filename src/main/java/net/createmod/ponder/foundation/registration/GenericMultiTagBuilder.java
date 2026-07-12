package net.createmod.ponder.foundation.registration;

import net.createmod.ponder.api.registration.MultiTagBuilder;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.util.ResourceLocation;

public final class GenericMultiTagBuilder {
    private GenericMultiTagBuilder() {
    }

    static final class TagImpl<T> implements MultiTagBuilder.Tag<T> {
        private final PonderTagRegistrationHelper<T> helper;
        private final Iterable<ResourceLocation> tags;

        TagImpl(PonderTagRegistrationHelper<T> helper, Iterable<ResourceLocation> tags) {
            this.helper = helper;
            this.tags = tags;
        }

        @Override public MultiTagBuilder.Tag<T> add(T component) {
            for (ResourceLocation tag : tags) helper.addTagToComponent(component, tag);
            return this;
        }
    }

    static final class ComponentImpl<T> implements MultiTagBuilder.Component {
        private final PonderTagRegistrationHelper<T> helper;
        private final Iterable<T> components;

        ComponentImpl(PonderTagRegistrationHelper<T> helper, Iterable<T> components) {
            this.helper = helper;
            this.components = components;
        }

        @Override public MultiTagBuilder.Component add(ResourceLocation tag) {
            for (T component : components) helper.addTagToComponent(component, tag);
            return this;
        }
    }
}
