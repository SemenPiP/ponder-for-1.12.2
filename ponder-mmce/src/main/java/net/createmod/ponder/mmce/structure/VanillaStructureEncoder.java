package net.createmod.ponder.mmce.structure;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.createmod.ponder.mmce.script.MMCEStructureRef;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

public final class VanillaStructureEncoder {
    private static final int DATA_VERSION_1_12_2 = 1343;
    private static final Comparator<BlockPos> POSITION_ORDER = Comparator
        .comparingInt(BlockPos::getX)
        .thenComparingInt(BlockPos::getY)
        .thenComparingInt(BlockPos::getZ);

    private VanillaStructureEncoder() {
    }

    static StructurePayload encode(MMCEStructureRef ref, List<SampledBlock> inputBlocks,
                                   Map<String, List<BlockPos>> inputGroups,
                                   List<String> inputDiagnostics) throws IOException {
        List<String> diagnostics = new ArrayList<String>(inputDiagnostics);
        Map<BlockPos, SampledBlock> uniqueBlocks = new LinkedHashMap<BlockPos, SampledBlock>();
        for (SampledBlock block : inputBlocks) {
            SampledBlock previous = uniqueBlocks.put(block.position, block);
            if (previous != null)
                diagnostics.add("Duplicate MMCE block position " + block.position + "; last entry retained");
        }

        List<SampledBlock> blocks = new ArrayList<SampledBlock>(uniqueBlocks.values());
        if (blocks.isEmpty())
            throw new IOException("MMCE block array is empty");

        Bounds bounds = Bounds.from(blocks, inputGroups);
        BlockPos offset = new BlockPos(-bounds.minX, -bounds.minY, -bounds.minZ);
        blocks.sort((left, right) -> POSITION_ORDER.compare(
            left.position.add(offset), right.position.add(offset)));

        Map<StateKey, IBlockState> states = new TreeMap<StateKey, IBlockState>();
        for (SampledBlock block : blocks)
            states.put(StateKey.of(block.state), block.state);

        Map<StateKey, Integer> paletteIndices = new LinkedHashMap<StateKey, Integer>();
        NBTTagList palette = new NBTTagList();
        int stateIndex = 0;
        for (Map.Entry<StateKey, IBlockState> state : states.entrySet()) {
            paletteIndices.put(state.getKey(), stateIndex++);
            palette.appendTag(writePaletteEntry(state.getValue(), diagnostics));
        }

        NBTTagList blockList = new NBTTagList();
        for (SampledBlock block : blocks) {
            BlockPos normalized = block.position.add(offset);
            NBTTagCompound entry = new NBTTagCompound();
            entry.setTag("pos", intVector(normalized));
            entry.setInteger("state", paletteIndices.get(StateKey.of(block.state)));
            if (block.previewNbt != null) {
                NBTTagCompound preview = block.previewNbt.copy();
                preview.setInteger("x", normalized.getX());
                preview.setInteger("y", normalized.getY());
                preview.setInteger("z", normalized.getZ());
                entry.setTag("nbt", preview);
            }
            blockList.appendTag(entry);
        }

        BlockPos size = new BlockPos(
            bounds.maxX - bounds.minX + 1,
            bounds.maxY - bounds.minY + 1,
            bounds.maxZ - bounds.minZ + 1
        );
        NBTTagCompound root = new NBTTagCompound();
        root.setTag("size", intVector(size));
        root.setTag("palette", palette);
        root.setTag("blocks", blockList);
        root.setTag("entities", new NBTTagList());
        root.setInteger("DataVersion", DATA_VERSION_1_12_2);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CompressedStreamTools.writeCompressed(root, output);
        byte[] nbtBytes = output.toByteArray();
        Map<String, List<BlockPos>> normalizedGroups = normalizeGroups(inputGroups, offset, diagnostics);
        BlockPos controller = BlockPos.ORIGIN.add(offset);
        return new StructurePayload(ref.asResourceLocation(), nbtBytes,
            fingerprint(nbtBytes, ref, normalizedGroups), normalizedGroups, diagnostics, size, controller);
    }

    private static Map<String, List<BlockPos>> normalizeGroups(Map<String, List<BlockPos>> inputGroups,
                                                                BlockPos offset, List<String> diagnostics) {
        Map<String, List<BlockPos>> normalized = new LinkedHashMap<String, List<BlockPos>>();
        Map<String, List<BlockPos>> sortedGroups = new TreeMap<String, List<BlockPos>>(inputGroups);
        for (Map.Entry<String, List<BlockPos>> entry : sortedGroups.entrySet()) {
            String name = entry.getKey();
            if (name == null || name.trim().isEmpty()) {
                diagnostics.add("Ignored an MMCE selector tag with an empty name");
                continue;
            }
            Set<BlockPos> positions = new LinkedHashSet<BlockPos>();
            for (BlockPos position : entry.getValue()) {
                if (position != null) positions.add(position.add(offset));
            }
            List<BlockPos> sorted = new ArrayList<BlockPos>(positions);
            sorted.sort(POSITION_ORDER);
            normalized.put(name, sorted);
        }
        return normalized;
    }

    private static NBTTagCompound writePaletteEntry(IBlockState state, List<String> diagnostics) {
        NBTTagCompound entry = new NBTTagCompound();
        ResourceLocation blockId = state.getBlock().getRegistryName();
        if (blockId == null) {
            blockId = new ResourceLocation("minecraft", "barrier");
            diagnostics.add("Encountered an unregistered sampled block; palette name replaced with minecraft:barrier");
        }
        entry.setString("Name", blockId.toString());

        NBTTagCompound properties = new NBTTagCompound();
        List<IProperty<?>> sorted = new ArrayList<IProperty<?>>(state.getPropertyKeys());
        sorted.sort(Comparator.comparing(IProperty::getName));
        for (IProperty<?> property : sorted)
            properties.setString(property.getName(), propertyValueName(property, state.getValue(property)));
        if (!properties.getKeySet().isEmpty()) entry.setTag("Properties", properties);
        return entry;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String propertyValueName(IProperty property, Comparable value) {
        return property.getName(value);
    }

    private static NBTTagList intVector(BlockPos value) {
        NBTTagList list = new NBTTagList();
        list.appendTag(new NBTTagInt(value.getX()));
        list.appendTag(new NBTTagInt(value.getY()));
        list.appendTag(new NBTTagInt(value.getZ()));
        return list;
    }

    private static String sha256(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest)
                result.append(String.format(Locale.ROOT, "%02x", value & 0xff));
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static String fingerprint(byte[] nbtBytes, MMCEStructureRef ref,
                                      Map<String, List<BlockPos>> groups) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(ref.canonicalSpec().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(nbtBytes);
            for (Map.Entry<String, List<BlockPos>> entry : groups.entrySet()) {
                digest.update((byte) 0);
                digest.update(entry.getKey().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                for (BlockPos position : entry.getValue()) {
                    digest.update((byte) 0);
                    digest.update(Integer.toString(position.getX())
                        .getBytes(java.nio.charset.StandardCharsets.US_ASCII));
                    digest.update((byte) ',');
                    digest.update(Integer.toString(position.getY())
                        .getBytes(java.nio.charset.StandardCharsets.US_ASCII));
                    digest.update((byte) ',');
                    digest.update(Integer.toString(position.getZ())
                        .getBytes(java.nio.charset.StandardCharsets.US_ASCII));
                }
            }
            StringBuilder result = new StringBuilder(64);
            for (byte value : digest.digest())
                result.append(String.format(Locale.ROOT, "%02x", value & 0xff));
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    public static final class SampledBlock {
        final BlockPos position;
        final IBlockState state;
        final NBTTagCompound previewNbt;

        public SampledBlock(BlockPos position, IBlockState state, NBTTagCompound previewNbt) {
            if (position == null) throw new IllegalArgumentException("position is required");
            if (state == null) throw new IllegalArgumentException("state is required");
            this.position = position.toImmutable();
            this.state = state;
            this.previewNbt = previewNbt == null ? null : previewNbt.copy();
        }
    }

    private static final class StateKey implements Comparable<StateKey> {
        private final String serialized;

        private StateKey(String serialized) {
            this.serialized = serialized;
        }

        static StateKey of(IBlockState state) {
            ResourceLocation id = state.getBlock().getRegistryName();
            StringBuilder value = new StringBuilder(id == null ? "minecraft:barrier" : id.toString());
            List<IProperty<?>> properties = new ArrayList<IProperty<?>>(state.getPropertyKeys());
            properties.sort(Comparator.comparing(IProperty::getName));
            for (IProperty<?> property : properties)
                value.append('|').append(property.getName()).append('=')
                    .append(propertyValueName(property, state.getValue(property)));
            return new StateKey(value.toString());
        }

        @Override
        public int compareTo(StateKey other) {
            return serialized.compareTo(other.serialized);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof StateKey && serialized.equals(((StateKey) other).serialized);
        }

        @Override
        public int hashCode() {
            return serialized.hashCode();
        }
    }

    private static final class Bounds {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        static Bounds from(List<SampledBlock> blocks, Map<String, List<BlockPos>> groups) {
            Bounds bounds = new Bounds();
            for (SampledBlock block : blocks) bounds.include(block.position);
            for (List<BlockPos> positions : groups.values())
                for (BlockPos position : positions)
                    if (position != null) bounds.include(position);
            return bounds;
        }

        void include(BlockPos position) {
            minX = Math.min(minX, position.getX());
            minY = Math.min(minY, position.getY());
            minZ = Math.min(minZ, position.getZ());
            maxX = Math.max(maxX, position.getX());
            maxY = Math.max(maxY, position.getY());
            maxZ = Math.max(maxZ, position.getZ());
        }
    }
}
