package net.createmod.ponder.mmce.structure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import github.kasuminova.mmce.common.util.DynamicPattern;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentSelectorTag;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import hellfirepvp.modularmachinery.common.machine.TaggedPositionBlockArray;
import hellfirepvp.modularmachinery.common.util.BlockArray;
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
        DynamicMachine machine = findMachine(ref.getMachineResourceLocation());
        if (!hasBlocks(machine))
            machine = MMCEPreviewMachineLoader.find(ref.getMachineResourceLocation());
        if (!hasBlocks(machine))
            throw new IOException("MMCE machine structure is not available: " + ref.machineId);

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

    private static DynamicMachine findMachine(ResourceLocation machineId) {
        DynamicMachine registered = MachineRegistry.getRegistry().getMachine(machineId);
        if (hasBlocks(registered)) return registered;
        DynamicMachine loaded = findMachine(MachineRegistry.getLoadedMachines(), machineId);
        if (hasBlocks(loaded)) return loaded;
        return findMachine(MachineRegistry.getWaitForLoadMachines(), machineId);
    }

    static DynamicMachine findMachine(Iterable<DynamicMachine> machines, ResourceLocation machineId) {
        if (machines == null || machineId == null) return null;
        for (DynamicMachine machine : machines)
            if (machine != null && machineId.equals(machine.getRegistryName()))
                return machine;
        return null;
    }

    private static boolean hasBlocks(DynamicMachine machine) {
        return machine != null && machine.getPattern() != null
            && machine.getPattern().getPattern() != null
            && !machine.getPattern().getPattern().isEmpty();
    }

    @Override
    public void invalidate() {
        synchronized (cache) {
            cache.clear();
        }
        MMCEPreviewMachineLoader.invalidate();
    }

    static TaggedPositionBlockArray expandDynamic(TaggedPositionBlockArray base, DynamicPattern pattern,
                                                  MMCEStructureRef ref) {
        if (base == null || pattern == null || ref == null)
            throw new IllegalArgumentException("MMCE dynamic expansion inputs are required");
        TaggedPositionBlockArray expanded = new TaggedPositionBlockArray(base);
        net.minecraft.util.EnumFacing facing = ref.getFacingValue();
        TaggedPositionBlockArray repeated = rotateTo(pattern.getPattern(), facing);
        TaggedPositionBlockArray ending = rotateTo(pattern.getPatternEnd(), facing);
        net.minecraft.util.math.BlockPos offset = pattern.getStructureSizeOffsetStart(facing);
        net.minecraft.util.math.BlockPos step = pattern.getStructureSizeOffset(facing);

        List<net.minecraft.util.math.BlockPos> offsets =
            dynamicOffsets(ref.repetitions, offset, step);
        for (int index = 0; index < offsets.size(); index++)
            addPattern(expanded, repeated, offsets.get(index), pattern.getName(), Integer.toString(index));
        if (ending != null)
            addPattern(expanded, ending,
                offsets.isEmpty() ? offset.add(step) : offsets.get(offsets.size() - 1).add(step),
                pattern.getName(), "end");
        return expanded;
    }

    static List<net.minecraft.util.math.BlockPos> dynamicOffsets(
            int repetitions, net.minecraft.util.math.BlockPos start,
            net.minecraft.util.math.BlockPos step) {
        if (repetitions < 0) throw new IllegalArgumentException("repetitions must not be negative");
        if (start == null || step == null)
            throw new IllegalArgumentException("dynamic offsets are required");
        List<net.minecraft.util.math.BlockPos> offsets =
            new ArrayList<net.minecraft.util.math.BlockPos>(repetitions);
        net.minecraft.util.math.BlockPos current = start;
        for (int index = 0; index < repetitions; index++) {
            if (index > 0) current = current.add(step);
            offsets.add(current);
        }
        return offsets;
    }

    private static TaggedPositionBlockArray rotateTo(TaggedPositionBlockArray source,
                                                     net.minecraft.util.EnumFacing facing) {
        if (source == null) return null;
        TaggedPositionBlockArray rotated = new TaggedPositionBlockArray(source);
        net.minecraft.util.EnumFacing current = net.minecraft.util.EnumFacing.NORTH;
        while (current != facing) {
            current = current.rotateYCCW();
            rotated = rotated.rotateYCCW();
        }
        return rotated;
    }

    private static void addPattern(TaggedPositionBlockArray target, TaggedPositionBlockArray source,
                                   net.minecraft.util.math.BlockPos offset, String patternName,
                                   String segment) {
        for (Map.Entry<net.minecraft.util.math.BlockPos, BlockArray.BlockInformation> entry
                : source.getPattern().entrySet())
            target.addBlock(entry.getKey().add(offset), entry.getValue());
        for (Map.Entry<net.minecraft.util.math.BlockPos, ComponentSelectorTag> entry
                : source.getTaggedPositions().entrySet()) {
            ComponentSelectorTag tag = entry.getValue();
            if (tag == null || tag.getTag() == null || tag.getTag().trim().isEmpty()) continue;
            target.setTag(entry.getKey().add(offset),
                new ComponentSelectorTag(dynamicTag(tag.getTag(), patternName, segment)));
        }
    }

    static String dynamicTag(String originalTag, String patternName, String segment) {
        if (originalTag == null || originalTag.trim().isEmpty()
            || patternName == null || patternName.trim().isEmpty()
            || segment == null || segment.trim().isEmpty())
            throw new IllegalArgumentException("MMCE dynamic tag parts are required");
        return originalTag + "_" + patternName + "_" + segment;
    }
}
