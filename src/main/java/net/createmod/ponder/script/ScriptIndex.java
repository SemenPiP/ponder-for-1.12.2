package net.createmod.ponder.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import crafttweaker.annotations.ZenRegister;
import net.minecraft.util.ResourceLocation;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.ponder.Index")
public final class ScriptIndex {
    private static final List<ResourceLocation> ZS_EXCLUDED = new ArrayList<ResourceLocation>();
    private static final List<ResourceLocation> JSON_EXCLUDED = new ArrayList<ResourceLocation>();

    private ScriptIndex() {
    }

    @ZenMethod
    public static synchronized void exclude(String itemId) {
        ResourceLocation id = ScriptSceneRegistry.parseId(itemId, "excluded item id");
        if (!ZS_EXCLUDED.contains(id)) ZS_EXCLUDED.add(id);
    }

    static synchronized List<ResourceLocation> snapshot() {
        List<ResourceLocation> result = new ArrayList<ResourceLocation>(ZS_EXCLUDED);
        for (ResourceLocation id : JSON_EXCLUDED)
            if (!result.contains(id)) result.add(id);
        return Collections.unmodifiableList(result);
    }

    static synchronized List<ResourceLocation> jsonSnapshot() {
        return Collections.unmodifiableList(new ArrayList<ResourceLocation>(JSON_EXCLUDED));
    }

    static synchronized void replaceJson(List<ResourceLocation> values) {
        if (values == null)
            throw new IllegalArgumentException("Local JSON index exclusions are required");
        List<ResourceLocation> replacement = new ArrayList<ResourceLocation>();
        for (ResourceLocation value : values) {
            if (value == null)
                throw new IllegalArgumentException("Local JSON index exclusion may not be null");
            if (!replacement.contains(value)) replacement.add(value);
        }
        JSON_EXCLUDED.clear();
        JSON_EXCLUDED.addAll(replacement);
    }
}
