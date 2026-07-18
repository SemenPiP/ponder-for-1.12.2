package net.createmod.ponder.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.util.ResourceLocation;

final class ScriptTagDefinition {
    final ResourceLocation id;
    final ResourceLocation icon;
    final String title;
    final String description;
    final boolean indexed;
    final List<ResourceLocation> components;

    ScriptTagDefinition(ResourceLocation id, ResourceLocation icon, String title, String description,
                        boolean indexed, List<ResourceLocation> components) {
        if (id == null || icon == null)
            throw new IllegalArgumentException("Script tag id and icon are required");
        if (id.toString().length() > 256 || icon.toString().length() > 256)
            throw new IllegalArgumentException("Script tag resource ids may not exceed 256 characters");
        if (title == null || title.length() > ScriptSceneSnapshot.MAX_TEXT_LENGTH
            || description == null || description.length() > ScriptSceneSnapshot.MAX_TEXT_LENGTH)
            throw new IllegalArgumentException("Script tag text is missing or too long: " + id);
        if (components == null)
            throw new IllegalArgumentException("Script tag components are required: " + id);
        if (components.size() > ScriptSceneSnapshot.MAX_TAG_COMPONENTS)
            throw new IllegalArgumentException("Script tag has too many components: " + id);
        this.id = id;
        this.icon = icon;
        this.title = title;
        this.description = description;
        this.indexed = indexed;
        this.components = Collections.unmodifiableList(new ArrayList<ResourceLocation>(components));
    }
}
