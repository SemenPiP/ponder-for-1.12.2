package net.createmod.ponder.mmce.structure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import github.kasuminova.mmce.common.util.DynamicPattern;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import hellfirepvp.modularmachinery.common.machine.TaggedPositionBlockArray;
import net.createmod.ponder.api.structure.PonderStructureProvider;
import net.createmod.ponder.api.structure.PonderStructureProviderResult;
import net.createmod.ponder.mmce.PonderMMCE;
import net.createmod.ponder.mmce.script.MMCEStructureRef;
import net.minecraft.util.ResourceLocation;

public final class MMCEStructureProvider implements PonderStructureProvider {
    public static final MMCEStructureProvider INSTANCE = new MMCEStructureProvider();
    public static final int PRIORITY = 200;
    private static final int MAX_CACHE_ENTRIES = 64;

    private final MMCEBlockArrayAdapter blockArrayAdapter = new MMCEBlockArrayAdapter();
    private final Map<ResourceLocation, StructurePayload> cache =
        new LinkedHashMap<ResourceLocation, StructurePayload>(16, .75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<ResourceLocation, StructurePayload> eldest) {
                return size() > MAX_CACHE_ENTRIES;
            }
        };

    private MMCEStructureProvider() {
    }

    @Override
    public ResourceLocation getId() {
        return PonderMMCE.PROVIDER_ID;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    public boolean supports(ResourceLocation structureId) {
        return MMCEStructureRef.tryParse(structureId) != null;
    }

    @Override
    public PonderStructureProviderResult find(ResourceLocation structureId) throws IOException {
        if (!supports(structureId)) return PonderStructureProviderResult.notFound();
        StructurePayload payload;
        synchronized (cache) {
            payload = cache.get(structureId);
        }
        if (payload == null) {
            payload = load(structureId);
            synchronized (cache) {
                cache.put(structureId, payload);
            }
        }
        return PonderStructureProviderResult.found(payload.getNbtBytes(), payload.getFingerprint(),
            payload.getNamedGroups(), payload.getDiagnostics());
    }

    public MMCEStructureRef createStaticReference(String machineId, boolean includePreviewNbt) {
        return resolveReference(MMCEStructureRef.unresolvedStatic(machineId, includePreviewNbt));
    }

    public MMCEStructureRef createDynamicReference(String machineId, String dynamicPattern, int repetitions,
                                                   String patternOffset, String facing,
                                                   boolean includePreviewNbt) {
        return resolveReference(MMCEStructureRef.unresolvedDynamic(machineId, dynamicPattern, repetitions,
            patternOffset, facing, includePreviewNbt));
    }

    private MMCEStructureRef resolveReference(MMCEStructureRef unresolved) {
        try {
            StructurePayload payload = load(unresolved);
            net.minecraft.util.math.BlockPos size = payload.getSize();
            net.minecraft.util.math.BlockPos controller = payload.getController();
            MMCEStructureRef resolved = unresolved.resolved(payload.getFingerprint(),
                size.getX(), size.getY(), size.getZ(),
                controller.getX(), controller.getY(), controller.getZ());
            synchronized (cache) {
                cache.put(resolved.asResourceLocation(), payload);
            }
            return resolved;
        } catch (IOException failure) {
            throw new IllegalArgumentException("Could not create Ponder-MMCE structure for "
                + unresolved.machineId + ": " + failure.getMessage(), failure);
        }
    }

    StructurePayload load(ResourceLocation structureId) throws IOException {
        MMCEStructureRef ref = MMCEStructureRef.tryParse(structureId);
        if (ref == null) throw new IOException("Unsupported Ponder-MMCE structure id: " + structureId);
        StructurePayload payload = load(ref);
        if (!ref.fingerprint.equals(payload.getFingerprint()))
            throw new IOException("MMCE structure fingerprint mismatch for " + ref.machineId
                + ": expected " + ref.fingerprint + ", local " + payload.getFingerprint());
        return payload;
    }

    private StructurePayload load(MMCEStructureRef ref) throws IOException {
        DynamicMachine machine = MachineRegistry.getRegistry().getMachine(ref.getMachineResourceLocation());
        if (machine == null)
            throw new IOException("MMCE machine is not loaded: " + ref.machineId);
        if (machine.getPattern() == null)
            throw new IOException("MMCE machine has no static BlockArray: " + ref.machineId);

        TaggedPositionBlockArray source;
        List<String> diagnostics = new ArrayList<String>();
        if (!ref.dynamic) {
            source = machine.getPattern();
        } else {
            DynamicPattern dynamicPattern = machine.getDynamicPatternByName(ref.dynamicPattern);
            if (dynamicPattern == null)
                throw new IOException("MMCE machine " + ref.machineId + " has no dynamic pattern '"
                    + ref.dynamicPattern + "'");
            if (ref.repetitions < dynamicPattern.getMinSize()
                || ref.repetitions > dynamicPattern.getMaxSize()) {
                throw new IOException("MMCE dynamic pattern '" + ref.dynamicPattern + "' requires "
                    + dynamicPattern.getMinSize() + ".." + dynamicPattern.getMaxSize()
                    + " repetitions, got " + ref.repetitions);
            }
            if (!dynamicPattern.getFaces().isEmpty()
                && !dynamicPattern.getFaces().contains(ref.getPatternOffsetValue()))
                throw new IOException("MMCE dynamic pattern '" + ref.dynamicPattern
                    + "' does not allow pattern offset " + ref.patternOffset);
            source = expandDynamic(machine.getPattern(), dynamicPattern, ref);
        }
        return blockArrayAdapter.convert(ref, source, diagnostics);
    }

    @Override
    public void invalidate() {
        synchronized (cache) {
            cache.clear();
        }
    }

    static TaggedPositionBlockArray expandDynamic(TaggedPositionBlockArray base, DynamicPattern pattern,
                                                  MMCEStructureRef ref) {
        return expandDynamic(base, ref, TaggedPositionBlockArray::new,
            (expanded, repetitions, dynamicFacing, machineFacing) ->
                pattern.addPatternToBlockArray(expanded, repetitions, dynamicFacing, machineFacing));
    }

    static TaggedPositionBlockArray expandDynamic(TaggedPositionBlockArray base, MMCEStructureRef ref,
                                                  BlockArrayCopier copier,
                                                  DynamicPatternInvoker invoker) {
        TaggedPositionBlockArray expanded = copier.copy(base);
        invoker.expand(expanded, ref.repetitions,
            ref.getPatternOffsetValue(), ref.getFacingValue());
        return expanded;
    }

    interface BlockArrayCopier {
        TaggedPositionBlockArray copy(TaggedPositionBlockArray source);
    }

    interface DynamicPatternInvoker {
        void expand(TaggedPositionBlockArray target, int repetitions,
                    net.minecraft.util.EnumFacing dynamicFacing,
                    net.minecraft.util.EnumFacing machineFacing);
    }
}
