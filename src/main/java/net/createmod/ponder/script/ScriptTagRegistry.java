package net.createmod.ponder.script;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import crafttweaker.annotations.ZenRegister;
import net.minecraft.util.ResourceLocation;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.ponder.TagRegistry")
public final class ScriptTagRegistry {
    private static final Map<ResourceLocation, ScriptTagDefinition> TAGS =
        new LinkedHashMap<ResourceLocation, ScriptTagDefinition>();

    private ScriptTagRegistry() {
    }

    @ZenMethod
    public static ScriptTagBuilder create(String id, String icon, String title, String description) {
        return new ScriptTagBuilder(id, icon, title, description);
    }

    static synchronized void register(ScriptTagDefinition definition) {
        if (TAGS.containsKey(definition.id))
            throw new IllegalArgumentException("Duplicate Ponder script tag id: " + definition.id);
        TAGS.put(definition.id, definition);
    }

    static synchronized Collection<ScriptTagDefinition> snapshot() {
        return Collections.unmodifiableCollection(new ArrayList<ScriptTagDefinition>(TAGS.values()));
    }
}
