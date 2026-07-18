package net.createmod.ponder.api.diagnostic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import net.minecraft.util.ResourceLocation;

/** Immutable modpack-facing description of a structure required by registered scenes. */
public final class PonderStructureDependency {
    private final ResourceLocation structureId;
    private final ResourceLocation providerId;
    private final String fingerprint;
    private final PonderStructureDependencyStatus status;
    private final List<ResourceLocation> sceneIds;
    private final List<ResourceLocation> components;
    private final List<PonderSceneSource> sources;
    private final ResourceLocation contributorId;

    public PonderStructureDependency(ResourceLocation structureId, ResourceLocation providerId,
                                     String fingerprint, PonderStructureDependencyStatus status,
                                     Collection<ResourceLocation> sceneIds,
                                     Collection<ResourceLocation> components,
                                     Collection<PonderSceneSource> sources,
                                     ResourceLocation contributorId) {
        if (structureId == null || status == null)
            throw new IllegalArgumentException("Ponder structure dependency identity is required");
        this.structureId = structureId;
        this.providerId = providerId;
        this.fingerprint = fingerprint == null ? "" : fingerprint;
        this.status = status;
        this.sceneIds = immutable(sceneIds);
        this.components = immutable(components);
        this.sources = immutableSources(sources);
        this.contributorId = contributorId;
    }

    public ResourceLocation getStructureId() { return structureId; }
    public ResourceLocation getProviderId() { return providerId; }
    public String getFingerprint() { return fingerprint; }
    public PonderStructureDependencyStatus getStatus() { return status; }
    public List<ResourceLocation> getSceneIds() { return sceneIds; }
    public List<ResourceLocation> getComponents() { return components; }
    public List<PonderSceneSource> getSources() { return sources; }
    public ResourceLocation getContributorId() { return contributorId; }

    private static List<ResourceLocation> immutable(Collection<ResourceLocation> source) {
        if (source == null || source.isEmpty())
            return Collections.emptyList();
        LinkedHashSet<ResourceLocation> unique = new LinkedHashSet<ResourceLocation>();
        for (ResourceLocation value : source) {
            if (value == null)
                throw new IllegalArgumentException("Ponder structure dependency ids may not contain null");
            unique.add(value);
        }
        return Collections.unmodifiableList(new ArrayList<ResourceLocation>(unique));
    }

    private static List<PonderSceneSource> immutableSources(Collection<PonderSceneSource> source) {
        if (source == null || source.isEmpty())
            return Collections.emptyList();
        LinkedHashSet<PonderSceneSource> unique = new LinkedHashSet<PonderSceneSource>();
        for (PonderSceneSource value : source) {
            if (value == null)
                throw new IllegalArgumentException("Ponder structure dependency sources may not contain null");
            unique.add(value);
        }
        return Collections.unmodifiableList(new ArrayList<PonderSceneSource>(unique));
    }
}
