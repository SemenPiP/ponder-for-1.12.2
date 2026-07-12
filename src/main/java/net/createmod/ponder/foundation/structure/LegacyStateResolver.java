package net.createmod.ponder.foundation.structure;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.common.base.Optional;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

/** Explicitly maps modern structure palette entries into the 1.12 block-state model. */
public final class LegacyStateResolver {
    private static final Map<String, String> RENAMED_BLOCKS;
    private static final List<String> WOODS = Arrays.asList("oak", "spruce", "birch", "jungle", "acacia", "dark_oak");
    private static final List<String> COLORS = Arrays.asList("white", "orange", "magenta", "light_blue", "yellow",
        "lime", "pink", "gray", "silver", "cyan", "purple", "blue", "brown", "green", "red", "black");

    static {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("minecraft:grass_block", "minecraft:grass");
        map.put("minecraft:dirt_path", "minecraft:grass_path");
        map.put("minecraft:cobweb", "minecraft:web");
        map.put("minecraft:short_grass", "minecraft:tallgrass");
        map.put("minecraft:bricks", "minecraft:brick_block");
        map.put("minecraft:nether_bricks", "minecraft:nether_brick");
        map.put("minecraft:red_nether_bricks", "minecraft:red_nether_brick");
        map.put("minecraft:terracotta", "minecraft:hardened_clay");
        map.put("minecraft:lily_pad", "minecraft:waterlily");
        map.put("minecraft:melon", "minecraft:melon_block");
        map.put("minecraft:sugar_cane", "minecraft:reeds");
        map.put("minecraft:spawner", "minecraft:mob_spawner");
        map.put("minecraft:note_block", "minecraft:noteblock");
        RENAMED_BLOCKS = Collections.unmodifiableMap(map);
    }

    public Resolution resolve(NBTTagCompound paletteEntry) {
        if (paletteEntry == null || !paletteEntry.hasKey("Name", 8))
            return Resolution.barrier("palette entry has no Name");
        String modernName = paletteEntry.getString("Name").toLowerCase(Locale.ROOT);
        MappedBlock mapped = mapBlock(modernName);
        if (mapped == null)
            return Resolution.barrier("no 1.12 mapping for " + modernName);
        ResourceLocation id;
        try {
            id = new ResourceLocation(mapped.legacyName);
        } catch (RuntimeException malformed) {
            return Resolution.barrier("malformed block id " + modernName);
        }
        Block block = Block.REGISTRY.getObject(id);
        if (block == null || block == Blocks.AIR && !"minecraft:air".equals(mapped.legacyName))
            return Resolution.barrier("mapped block is absent in 1.12: " + mapped.legacyName);
        IBlockState state = block.getDefaultState();
        StringBuilder warnings = new StringBuilder();
        for (Map.Entry<String, String> fixed : mapped.fixedProperties.entrySet())
            state = applyProperty(state, fixed.getKey(), fixed.getValue(), warnings, true);
        if (paletteEntry.hasKey("Properties", 10)) {
            NBTTagCompound properties = paletteEntry.getCompoundTag("Properties");
            for (String property : properties.getKeySet()) {
                if ("waterlogged".equals(property)) {
                    append(warnings, "dropped waterlogged property");
                    continue;
                }
                String legacyProperty = "type".equals(property) && hasProperty(state, "variant") ? "variant" : property;
                state = applyProperty(state, legacyProperty, normalizePropertyValue(property, properties.getString(property)),
                    warnings, false);
            }
        }
        return new Resolution(state, warnings.length() == 0 ? null : warnings.toString(), false);
    }

    private static MappedBlock mapBlock(String name) {
        String renamed = RENAMED_BLOCKS.get(name);
        if (renamed != null) {
            MappedBlock mapped = new MappedBlock(renamed);
            if ("minecraft:short_grass".equals(name)) mapped.fixedProperties.put("type", "grass");
            return mapped;
        }
        int separator = name.indexOf(':');
        String namespace = separator < 0 ? "minecraft" : name.substring(0, separator);
        String path = separator < 0 ? name : name.substring(separator + 1);
        if (!"minecraft".equals(namespace))
            return new MappedBlock(name);
        for (String wood : WOODS) {
            if (path.equals(wood + "_planks")) return variant("minecraft:planks", "variant", wood);
            if (path.equals(wood + "_sapling")) return variant("minecraft:sapling", "type", wood);
            if (path.equals(wood + "_log")) return variant(isSecondWoodSet(wood) ? "minecraft:log2" : "minecraft:log", "variant", wood);
            if (path.equals(wood + "_wood")) {
                MappedBlock result = variant(isSecondWoodSet(wood) ? "minecraft:log2" : "minecraft:log", "variant", wood);
                result.fixedProperties.put("axis", "none");
                return result;
            }
            if (path.equals(wood + "_leaves")) return variant(isSecondWoodSet(wood) ? "minecraft:leaves2" : "minecraft:leaves", "variant", wood);
        }
        for (String color : COLORS) {
            String modernColor = "silver".equals(color) ? "light_gray" : color;
            if (path.equals(modernColor + "_wool")) return variant("minecraft:wool", "color", color);
            if (path.equals(modernColor + "_stained_glass")) return variant("minecraft:stained_glass", "color", color);
            if (path.equals(modernColor + "_stained_glass_pane")) return variant("minecraft:stained_glass_pane", "color", color);
            if (path.equals(modernColor + "_concrete")) return variant("minecraft:concrete", "color", color);
            if (path.equals(modernColor + "_concrete_powder")) return variant("minecraft:concrete_powder", "color", color);
            if (path.equals(modernColor + "_terracotta")) return variant("minecraft:stained_hardened_clay", "color", color);
        }
        return new MappedBlock(name);
    }

    private static boolean isSecondWoodSet(String wood) {
        return "acacia".equals(wood) || "dark_oak".equals(wood);
    }

    private static MappedBlock variant(String block, String property, String value) {
        MappedBlock result = new MappedBlock(block);
        result.fixedProperties.put(property, value);
        return result;
    }

    private static String normalizePropertyValue(String property, String value) {
        if ("half".equals(property) && "lower".equals(value)) return "bottom";
        if ("half".equals(property) && "upper".equals(value)) return "top";
        return value;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static IBlockState applyProperty(IBlockState state, String name, String value,
                                             StringBuilder warnings, boolean fixed) {
        for (IProperty property : state.getPropertyKeys()) {
            if (!property.getName().equals(name)) continue;
            Optional<? extends Comparable> parsed = property.parseValue(value);
            if (parsed.isPresent()) return state.withProperty(property, parsed.get());
            append(warnings, "invalid value " + value + " for " + name);
            return state;
        }
        if (!fixed) append(warnings, "property " + name + " has no 1.12 equivalent");
        return state;
    }

    private static boolean hasProperty(IBlockState state, String name) {
        for (IProperty<?> property : state.getPropertyKeys())
            if (property.getName().equals(name)) return true;
        return false;
    }

    private static void append(StringBuilder builder, String warning) {
        if (builder.length() > 0) builder.append("; ");
        builder.append(warning);
    }

    private static final class MappedBlock {
        final String legacyName;
        final Map<String, String> fixedProperties = new LinkedHashMap<String, String>();
        MappedBlock(String legacyName) { this.legacyName = legacyName; }
    }

    public static final class Resolution {
        private final IBlockState state;
        private final String diagnostic;
        private final boolean substituted;

        private Resolution(IBlockState state, String diagnostic, boolean substituted) {
            this.state = state;
            this.diagnostic = diagnostic;
            this.substituted = substituted;
        }

        static Resolution barrier(String diagnostic) {
            return new Resolution(Blocks.BARRIER.getDefaultState(), diagnostic, true);
        }

        public IBlockState getState() { return state; }
        public String getDiagnostic() { return diagnostic; }
        public boolean isSubstituted() { return substituted; }
    }
}
