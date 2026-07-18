package net.createmod.ponder.api.script;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

/** Immutable network identity and capability description for a script instruction codec. */
public final class ScriptInstructionCodecDescriptor {
    public static final int MAX_CAPABILITIES = 64;
    public static final int MAX_ID_LENGTH = 256;

    private final ResourceLocation id;
    private final int protocolVersion;
    private final Set<ResourceLocation> capabilities;

    public ScriptInstructionCodecDescriptor(ResourceLocation id, int protocolVersion,
                                            Collection<ResourceLocation> capabilities) {
        if (id == null || id.toString().length() > MAX_ID_LENGTH)
            throw new IllegalArgumentException("Script instruction codec id is required and may not exceed "
                + MAX_ID_LENGTH + " characters");
        if (protocolVersion <= 0)
            throw new IllegalArgumentException("Script instruction codec protocol version must be positive: " + id);
        Collection<ResourceLocation> supplied = capabilities == null
            ? Collections.<ResourceLocation>emptyList() : capabilities;
        if (supplied.size() > MAX_CAPABILITIES)
            throw new IllegalArgumentException("Script instruction codec " + id + " exceeds "
                + MAX_CAPABILITIES + " capabilities");
        LinkedHashSet<ResourceLocation> unique = new LinkedHashSet<ResourceLocation>();
        for (ResourceLocation capability : supplied) {
            if (capability == null || capability.toString().length() > MAX_ID_LENGTH)
                throw new IllegalArgumentException("Invalid capability for script instruction codec " + id);
            if (!unique.add(capability))
                throw new IllegalArgumentException("Duplicate capability " + capability
                    + " for script instruction codec " + id);
        }
        List<ResourceLocation> sorted = new ArrayList<ResourceLocation>(unique);
        Collections.sort(sorted, Comparator.comparing(ResourceLocation::toString));
        this.id = id;
        this.protocolVersion = protocolVersion;
        this.capabilities = Collections.unmodifiableSet(new LinkedHashSet<ResourceLocation>(sorted));
    }

    public ResourceLocation getId() {
        return id;
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public Set<ResourceLocation> getCapabilities() {
        return capabilities;
    }

    public boolean satisfies(ScriptInstructionCodecDescriptor requirement) {
        return requirement != null && id.equals(requirement.id)
            && protocolVersion == requirement.protocolVersion
            && capabilities.containsAll(requirement.capabilities);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (!(object instanceof ScriptInstructionCodecDescriptor))
            return false;
        ScriptInstructionCodecDescriptor other = (ScriptInstructionCodecDescriptor) object;
        return protocolVersion == other.protocolVersion && id.equals(other.id)
            && capabilities.equals(other.capabilities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, protocolVersion, capabilities);
    }

    @Override
    public String toString() {
        return id + "@v" + protocolVersion + capabilities;
    }
}
