package net.createmod.ponder.api.structure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import net.minecraft.util.math.BlockPos;

public final class PonderStructureProviderResult {
    public enum Status {
        FOUND,
        NOT_FOUND
    }

    private static final PonderStructureProviderResult NOT_FOUND =
        new PonderStructureProviderResult(Status.NOT_FOUND, null, null,
            Collections.<String, Collection<BlockPos>>emptyMap(), Collections.<String>emptyList());

    private final Status status;
    private final byte[] nbtBytes;
    private final String fingerprint;
    private final Map<String, List<BlockPos>> groups;
    private final List<String> diagnostics;

    private PonderStructureProviderResult(Status status, byte[] nbtBytes, String fingerprint,
                                          Map<String, ? extends Collection<BlockPos>> groups,
                                          Collection<String> diagnostics) {
        this.status = status;
        this.nbtBytes = nbtBytes == null ? null : nbtBytes.clone();
        this.fingerprint = fingerprint;
        this.groups = immutableGroups(groups);
        this.diagnostics = immutableDiagnostics(diagnostics);
    }

    public static PonderStructureProviderResult found(byte[] nbtBytes, String fingerprint) {
        return found(nbtBytes, fingerprint, Collections.<String, Collection<BlockPos>>emptyMap(),
            Collections.<String>emptyList());
    }

    public static PonderStructureProviderResult found(byte[] nbtBytes, String fingerprint,
                                                       Map<String, ? extends Collection<BlockPos>> groups,
                                                       Collection<String> diagnostics) {
        if (nbtBytes == null || nbtBytes.length == 0)
            throw new IllegalArgumentException("Found structure data must contain NBT bytes");
        if (fingerprint == null || fingerprint.trim().isEmpty())
            throw new IllegalArgumentException("Found structure data must have a fingerprint");
        return new PonderStructureProviderResult(Status.FOUND, nbtBytes, fingerprint, groups, diagnostics);
    }

    public static PonderStructureProviderResult notFound() {
        return NOT_FOUND;
    }

    public static PonderStructureProviderResult notFound(Collection<String> diagnostics) {
        if (diagnostics == null || diagnostics.isEmpty())
            return NOT_FOUND;
        return new PonderStructureProviderResult(Status.NOT_FOUND, null, null,
            Collections.<String, Collection<BlockPos>>emptyMap(), diagnostics);
    }

    public Status getStatus() {
        return status;
    }

    public boolean isFound() {
        return status == Status.FOUND;
    }

    public byte[] getNbtBytes() {
        return nbtBytes == null ? null : nbtBytes.clone();
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public Map<String, List<BlockPos>> getGroups() {
        return groups;
    }

    public List<String> getDiagnostics() {
        return diagnostics;
    }

    private static Map<String, List<BlockPos>> immutableGroups(
        Map<String, ? extends Collection<BlockPos>> source) {
        if (source == null || source.isEmpty())
            return Collections.emptyMap();
        Map<String, List<BlockPos>> copy = new LinkedHashMap<String, List<BlockPos>>();
        for (Map.Entry<String, ? extends Collection<BlockPos>> entry : source.entrySet()) {
            String name = requireGroupName(entry.getKey());
            LinkedHashSet<BlockPos> positions = new LinkedHashSet<BlockPos>();
            Collection<BlockPos> supplied = entry.getValue();
            if (supplied != null) {
                for (BlockPos position : supplied) {
                    if (position == null)
                        throw new IllegalArgumentException("Structure group " + name + " contains a null position");
                    positions.add(position.toImmutable());
                }
            }
            copy.put(name, Collections.unmodifiableList(new ArrayList<BlockPos>(positions)));
        }
        return Collections.unmodifiableMap(copy);
    }

    private static List<String> immutableDiagnostics(Collection<String> source) {
        if (source == null || source.isEmpty())
            return Collections.emptyList();
        List<String> copy = new ArrayList<String>(source.size());
        for (String diagnostic : source) {
            if (diagnostic == null)
                throw new IllegalArgumentException("Structure diagnostics may not contain null");
            copy.add(diagnostic);
        }
        return Collections.unmodifiableList(copy);
    }

    private static String requireGroupName(String name) {
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("Structure group names may not be empty");
        if (name.length() > 256)
            throw new IllegalArgumentException("Structure group names may not exceed 256 characters");
        return name;
    }
}
