package net.createmod.ponder.script;

import java.util.ArrayList;
import java.util.List;

import crafttweaker.annotations.ZenRegister;
import net.minecraft.util.ResourceLocation;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.ponder.TagBuilder")
public final class ScriptTagBuilder {
    private final ResourceLocation id;
    private final ResourceLocation icon;
    private final String title;
    private final String description;
    private final List<ResourceLocation> components = new ArrayList<ResourceLocation>();
    private boolean indexed = true;
    private boolean registered;

    ScriptTagBuilder(String id, String icon, String title, String description) {
        this.id = ScriptSceneRegistry.parseId(id, "tag id");
        this.icon = ScriptSceneRegistry.parseId(icon, "tag icon");
        this.title = ScriptWorldBuilder.requiredText(title, "tag title");
        this.description = ScriptWorldBuilder.requiredText(description, "tag description");
    }

    @ZenMethod
    public ScriptTagBuilder addComponent(String component) {
        ensureMutable();
        ResourceLocation id = ScriptSceneRegistry.parseId(component, "component id");
        if (!components.contains(id)) components.add(id);
        return this;
    }

    @ZenMethod
    public ScriptTagBuilder indexed(boolean indexed) {
        ensureMutable();
        this.indexed = indexed;
        return this;
    }

    @ZenMethod
    public void register() {
        ensureMutable();
        registered = true;
        ScriptTagRegistry.register(new ScriptTagDefinition(id, icon, title, description, indexed, components));
    }

    private void ensureMutable() {
        if (registered) throw new IllegalStateException("Tag is already registered: " + id);
    }
}
