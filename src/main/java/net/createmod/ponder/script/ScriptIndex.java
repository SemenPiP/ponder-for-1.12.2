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
    private static final List<ResourceLocation> EXCLUDED = new ArrayList<ResourceLocation>();

    private ScriptIndex() {
    }

    @ZenMethod
    public static synchronized void exclude(String itemId) {
        ResourceLocation id = ScriptSceneRegistry.parseId(itemId, "excluded item id");
        if (!EXCLUDED.contains(id)) EXCLUDED.add(id);
    }

    static synchronized List<ResourceLocation> snapshot() {
        return Collections.unmodifiableList(new ArrayList<ResourceLocation>(EXCLUDED));
    }
}
