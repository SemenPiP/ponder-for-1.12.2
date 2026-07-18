package net.createmod.ponder.mmce.structure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

public final class StructurePayload {
    private final ResourceLocation structureId;
    private final byte[] nbtBytes;
    private final String fingerprint;
    private final Map<String, List<BlockPos>> namedGroups;
    private final List<String> diagnostics;
    private final BlockPos size;
    private final BlockPos controller;

    StructurePayload(ResourceLocation structureId, byte[] nbtBytes, String fingerprint,
                     Map<String, List<BlockPos>> namedGroups, List<String> diagnostics,
                     BlockPos size, BlockPos controller) {
        this.structureId = structureId;
        this.nbtBytes = nbtBytes.clone();
        this.fingerprint = fingerprint;
        Map<String, List<BlockPos>> groups = new LinkedHashMap<String, List<BlockPos>>();
        for (Map.Entry<String, List<BlockPos>> entry : namedGroups.entrySet())
            groups.put(entry.getKey(),
                Collections.unmodifiableList(new ArrayList<BlockPos>(entry.getValue())));
        this.namedGroups = Collections.unmodifiableMap(groups);
        this.diagnostics = Collections.unmodifiableList(new ArrayList<String>(diagnostics));
        this.size = size.toImmutable();
        this.controller = controller.toImmutable();
    }

    public ResourceLocation getStructureId() {
        return structureId;
    }

    public byte[] getNbtBytes() {
        return nbtBytes.clone();
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public Map<String, List<BlockPos>> getNamedGroups() {
        return namedGroups;
    }

    public List<String> getDiagnostics() {
        return diagnostics;
    }

    public BlockPos getSize() {
        return size;
    }

    public BlockPos getController() {
        return controller;
    }
}
