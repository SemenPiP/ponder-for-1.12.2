package net.createmod.ponder.foundation.registration;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.registration.SceneRegistryAccess;
import net.createmod.ponder.api.registration.StoryBoardEntry;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.PonderSceneBuildingUtil;
import net.createmod.ponder.foundation.structure.PonderStructure;
import net.createmod.ponder.foundation.structure.PonderStructureLoader;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

public final class PonderSceneRegistry implements SceneRegistryAccess {
    private static final Logger LOGGER = LogManager.getLogger("PonderSceneRegistry");
    private final PonderLocalization localization;
    private final PonderStructureLoader loader;
    private final Map<ResourceLocation, List<StoryBoardEntry>> scenes =
        new LinkedHashMap<ResourceLocation, List<StoryBoardEntry>>();
    private boolean frozen;

    public PonderSceneRegistry(PonderLocalization localization) {
        this(localization, new PonderStructureLoader());
    }

    public PonderSceneRegistry(PonderLocalization localization, PonderStructureLoader loader) {
        this.localization = localization;
        this.loader = loader;
    }

    public synchronized void clearRegistry() {
        scenes.clear();
        frozen = false;
    }

    public synchronized void freeze() {
        frozen = true;
    }

    public synchronized void addStoryBoard(StoryBoardEntry entry) {
        if (frozen) throw new IllegalStateException("Ponder scene registration is frozen");
        List<StoryBoardEntry> entries = scenes.get(entry.getComponent());
        if (entries == null) {
            entries = new ArrayList<StoryBoardEntry>();
            scenes.put(entry.getComponent(), entries);
        }
        entries.add(entry);
    }

    @Override public synchronized boolean doScenesExistForId(ResourceLocation id) {
        List<StoryBoardEntry> entries = scenes.get(id);
        return entries != null && !entries.isEmpty();
    }

    @Override public synchronized Collection<Map.Entry<ResourceLocation, StoryBoardEntry>> getRegisteredEntries() {
        List<Map.Entry<ResourceLocation, StoryBoardEntry>> result =
            new ArrayList<Map.Entry<ResourceLocation, StoryBoardEntry>>();
        for (Map.Entry<ResourceLocation, List<StoryBoardEntry>> group : scenes.entrySet())
            for (StoryBoardEntry entry : group.getValue())
                result.add(new AbstractMap.SimpleImmutableEntry<ResourceLocation, StoryBoardEntry>(group.getKey(), entry));
        return Collections.unmodifiableList(result);
    }

    private static List<PonderScene> applyOrdering(List<PonderScene> scenes) {
        Map<ResourceLocation, PonderScene> byId = new LinkedHashMap<ResourceLocation, PonderScene>();
        Map<PonderScene, Set<PonderScene>> outgoing = new LinkedHashMap<PonderScene, Set<PonderScene>>();
        Map<PonderScene, Integer> incoming = new LinkedHashMap<PonderScene, Integer>();
        for (PonderScene scene : scenes) { byId.put(scene.getId(), scene); outgoing.put(scene, new LinkedHashSet<PonderScene>()); incoming.put(scene, 0); }
        for (PonderScene scene : scenes) {
            for (StoryBoardEntry.SceneOrderingEntry order : scene.getOrderingEntries()) {
                PonderScene other = byId.get(order.getSceneId());
                if (other == null || other == scene) continue;
                PonderScene before = order.getType() == StoryBoardEntry.SceneOrderingType.BEFORE ? scene : other;
                PonderScene after = before == scene ? other : scene;
                if (outgoing.get(before).add(after)) incoming.put(after, incoming.get(after) + 1);
            }
        }
        List<PonderScene> ordered = new ArrayList<PonderScene>();
        Set<PonderScene> emitted = new LinkedHashSet<PonderScene>();
        while (ordered.size() < scenes.size()) {
            PonderScene next = null;
            for (PonderScene candidate : scenes)
                if (!emitted.contains(candidate) && incoming.get(candidate) == 0) { next = candidate; break; }
            if (next == null) {
                for (PonderScene candidate : scenes) if (!emitted.contains(candidate)) { next = candidate; break; }
            }
            emitted.add(next); ordered.add(next);
            for (PonderScene target : outgoing.get(next)) incoming.put(target, Math.max(0, incoming.get(target) - 1));
        }
        return ordered;
    }

    @Override public synchronized List<PonderScene> compile(ResourceLocation id) {
        List<StoryBoardEntry> entries = scenes.get(id);
        if (entries == null || entries.isEmpty()) return Collections.emptyList();
        return compile(new ArrayList<StoryBoardEntry>(entries));
    }

    @Override public List<PonderScene> compile(Collection<StoryBoardEntry> entries) {
        List<PonderScene> result = new ArrayList<PonderScene>();
        for (StoryBoardEntry entry : entries) {
            PonderStructure structure;
            try {
                structure = loader.load(entry.getSchematicLocation());
            } catch (IOException exception) {
                LOGGER.error("Failed to load Ponder structure {}", entry.getSchematicLocation(), exception);
                structure = PonderStructure.missing("failed to load " + entry.getSchematicLocation() + ": " + exception.getMessage());
            }
            if (!structure.getDiagnostics().isEmpty())
                LOGGER.warn("Ponder structure {} loaded with {} diagnostic(s): {}", entry.getSchematicLocation(),
                    structure.getDiagnostics().size(), structure.getDiagnostics());
            PonderLevel world = new PonderLevel(BlockPos.ORIGIN, null);
            structure.place(world);
            world.createBackup();
            PonderScene scene = compileScene(localization, entry, world);
            scene.begin();
            result.add(scene);
        }
        return Collections.unmodifiableList(applyOrdering(result));
    }

    public static PonderScene compileScene(PonderLocalization localization, StoryBoardEntry entry,
                                           PonderLevel level) {
        PonderScene scene = new PonderScene(level, localization, entry.getNamespace(), entry.getComponent(),
            entry.getTags(), entry.getOrderingEntries());
        SceneBuildingUtil util;
        if (level == null) {
            util = scene.getSceneBuildingUtil();
        } else {
            util = new PonderSceneBuildingUtil(level.getBoundsMin(), level.getBoundsMax(),
                level.getStructureGroups(), entry.getSchematicLocation());
        }
        entry.getBoard().program(scene.builder(), util);
        return scene;
    }

    public PonderStructure loadSchematic(ResourceLocation location) throws IOException {
        return loader.load(location);
    }
}
