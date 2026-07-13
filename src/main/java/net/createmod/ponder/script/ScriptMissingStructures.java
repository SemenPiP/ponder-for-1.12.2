package net.createmod.ponder.script;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.createmod.ponder.foundation.structure.PonderStructureLoader;
import net.minecraft.util.ResourceLocation;

public final class ScriptMissingStructures {
    private static final Set<String> PENDING = new LinkedHashSet<String>();

    private ScriptMissingStructures() {
    }

    public static synchronized void record(ResourceLocation sceneId, ResourceLocation structureId) {
        PENDING.add("Skipped Ponder scene " + sceneId + ": missing structure " + structureId
            + " (expected " + PonderStructureLoader.expectedExternalPath(structureId) + ")");
    }

    public static synchronized List<String> drain() {
        List<String> result = new ArrayList<String>(PENDING);
        PENDING.clear();
        return result;
    }
}
