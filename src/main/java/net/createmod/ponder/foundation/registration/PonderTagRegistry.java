package net.createmod.ponder.foundation.registration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.createmod.ponder.api.registration.TagRegistryAccess;
import net.createmod.ponder.foundation.PonderTag;
import net.minecraft.util.ResourceLocation;

public final class PonderTagRegistry implements TagRegistryAccess {
    private final Map<ResourceLocation, PonderTag> tags = new LinkedHashMap<ResourceLocation, PonderTag>();
    private final Map<ResourceLocation, LinkedHashSet<ResourceLocation>> componentTags =
        new LinkedHashMap<ResourceLocation, LinkedHashSet<ResourceLocation>>();
    private final List<PonderTag> listedTags = new ArrayList<PonderTag>();
    private boolean frozen;

    public synchronized void clearRegistry() {
        tags.clear();
        componentTags.clear();
        listedTags.clear();
        frozen = false;
    }

    public synchronized void freeze() {
        frozen = true;
    }

    public synchronized void registerTag(PonderTag tag) {
        checkMutable();
        if (tags.containsKey(tag.getId()))
            throw new IllegalArgumentException("Ponder tag already registered: " + tag.getId());
        tags.put(tag.getId(), tag);
    }

    public synchronized void listTag(PonderTag tag) {
        checkMutable();
        if (!listedTags.contains(tag))
            listedTags.add(tag);
    }

    public synchronized void addTagToComponent(ResourceLocation tag, ResourceLocation component) {
        checkMutable();
        LinkedHashSet<ResourceLocation> assigned = componentTags.get(component);
        if (assigned == null) {
            assigned = new LinkedHashSet<ResourceLocation>();
            componentTags.put(component, assigned);
        }
        assigned.add(tag);
    }

    private void checkMutable() {
        if (frozen)
            throw new IllegalStateException("Ponder tag registration is frozen");
    }

    @Override
    public synchronized PonderTag getRegisteredTag(ResourceLocation location) {
        PonderTag tag = tags.get(location);
        return tag == null ? PonderTag.missing(location) : tag;
    }

    @Override
    public synchronized List<PonderTag> getListedTags() {
        return Collections.unmodifiableList(new ArrayList<PonderTag>(listedTags));
    }

    @Override
    public synchronized Set<PonderTag> getTags(ResourceLocation component) {
        LinkedHashSet<ResourceLocation> assigned = componentTags.get(component);
        if (assigned == null)
            return Collections.emptySet();
        LinkedHashSet<PonderTag> result = new LinkedHashSet<PonderTag>();
        for (ResourceLocation location : assigned)
            result.add(getRegisteredTag(location));
        return Collections.unmodifiableSet(result);
    }

    @Override
    public synchronized Set<ResourceLocation> getItems(ResourceLocation tag) {
        LinkedHashSet<ResourceLocation> result = new LinkedHashSet<ResourceLocation>();
        for (Map.Entry<ResourceLocation, LinkedHashSet<ResourceLocation>> entry : componentTags.entrySet())
            if (entry.getValue().contains(tag))
                result.add(entry.getKey());
        return Collections.unmodifiableSet(result);
    }

    @Override
    public Set<ResourceLocation> getItems(PonderTag tag) {
        return tag == null ? Collections.<ResourceLocation>emptySet() : getItems(tag.getId());
    }
}
