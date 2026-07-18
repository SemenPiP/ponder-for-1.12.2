package net.createmod.ponder.mmce.structure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import hellfirepvp.modularmachinery.common.crafting.helper.ComponentSelectorTag;
import hellfirepvp.modularmachinery.common.machine.TaggedPositionBlockArray;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import net.createmod.ponder.mmce.script.MMCEStructureRef;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

public final class MMCEBlockArrayAdapter {
    private static final Comparator<Map.Entry<BlockPos, BlockArray.BlockInformation>> ENTRY_ORDER =
        Comparator.comparingInt((Map.Entry<BlockPos, BlockArray.BlockInformation> entry) -> entry.getKey().getX())
            .thenComparingInt(entry -> entry.getKey().getY())
            .thenComparingInt(entry -> entry.getKey().getZ());

    public StructurePayload convert(MMCEStructureRef ref, TaggedPositionBlockArray blockArray,
                                    List<String> initialDiagnostics) throws IOException {
        if (ref == null) throw new IllegalArgumentException("MMCE structure reference is required");
        if (blockArray == null) throw new IllegalArgumentException("MMCE block array is required");

        List<String> diagnostics = new ArrayList<String>(initialDiagnostics);
        List<Map.Entry<BlockPos, BlockArray.BlockInformation>> entries =
            new ArrayList<Map.Entry<BlockPos, BlockArray.BlockInformation>>(blockArray.getPattern().entrySet());
        entries.sort(ENTRY_ORDER);
        if (entries.isEmpty())
            throw new IOException("MMCE machine structure contains no blocks: " + ref.machineId);

        List<VanillaStructureEncoder.SampledBlock> blocks =
            new ArrayList<VanillaStructureEncoder.SampledBlock>(entries.size());
        for (Map.Entry<BlockPos, BlockArray.BlockInformation> entry : entries) {
            BlockPos position = entry.getKey().toImmutable();
            BlockArray.BlockInformation information = entry.getValue();
            if (information == null)
                throw new IOException("MMCE block " + position + " has no BlockInformation");

            IBlockState sampled;
            long sampleSeed = position.toLong();
            try {
                sampled = information.getSampleState(sampleSeed);
                if (sampled == null) throw new IllegalStateException("sample state is null");
            } catch (RuntimeException failedSample) {
                throw new IOException("MMCE sampleState(" + sampleSeed + ") failed at " + position
                    + " for " + ref.machineId, failedSample);
            }

            NBTTagCompound preview = null;
            if (ref.includePreviewNbt) {
                NBTTagCompound source = information.getPreviewTag();
                if (source != null) preview = source.copy();
            }
            blocks.add(new VanillaStructureEncoder.SampledBlock(position, sampled, preview));
        }

        Map<String, List<BlockPos>> groups = new LinkedHashMap<String, List<BlockPos>>();
        List<BlockPos> all = new ArrayList<BlockPos>(entries.size());
        for (Map.Entry<BlockPos, BlockArray.BlockInformation> entry : entries)
            all.add(entry.getKey().toImmutable());
        groups.put("mmce:all", all);
        groups.put("mmce:controller", java.util.Collections.singletonList(BlockPos.ORIGIN));
        for (Map.Entry<BlockPos, ComponentSelectorTag> entry : blockArray.getTaggedPositions().entrySet()) {
            ComponentSelectorTag selector = entry.getValue();
            String name = selector == null ? "" : selector.getTag();
            if (name == null || name.trim().isEmpty()) {
                diagnostics.add("Ignored an MMCE selector tag with an empty name at " + entry.getKey());
                continue;
            }
            BlockPos position = entry.getKey().toImmutable();
            add(groups, "mmce:tag/" + name, position);
            addDynamicAliases(ref, groups, name, position);
        }
        return VanillaStructureEncoder.encode(ref, blocks, groups, diagnostics);
    }

    private static void addDynamicAliases(MMCEStructureRef ref, Map<String, List<BlockPos>> groups,
                                          String generatedTag, BlockPos position) {
        if (!ref.dynamic) return;
        String prefix = ref.dynamicPattern + "_";
        if (!generatedTag.startsWith(prefix)) return;
        String body = generatedTag.substring(prefix.length());
        if (body.endsWith("_end")) {
            String tag = body.substring(0, body.length() - 4);
            if (!tag.isEmpty())
                add(groups, "mmce:dynamic/" + ref.dynamicPattern + "/end/" + tag, position);
            return;
        }
        int separator = body.lastIndexOf('_');
        if (separator <= 0 || separator == body.length() - 1) return;
        String index = body.substring(separator + 1);
        try {
            int parsed = Integer.parseInt(index);
            if (parsed < 0) return;
        } catch (NumberFormatException ignored) {
            return;
        }
        String tag = body.substring(0, separator);
        add(groups, "mmce:dynamic/" + ref.dynamicPattern + "/segment/" + index + "/" + tag, position);
    }

    private static void add(Map<String, List<BlockPos>> groups, String name, BlockPos position) {
        groups.computeIfAbsent(name, ignored -> new ArrayList<BlockPos>()).add(position);
    }
}
