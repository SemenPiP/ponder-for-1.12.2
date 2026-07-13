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
        this.id = id; this.icon = icon; this.title = title; this.description = description; this.indexed = indexed;
        this.components = Collections.unmodifiableList(new ArrayList<ResourceLocation>(components));
    }
}
