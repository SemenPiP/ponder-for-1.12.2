package net.createmod.ponder.script;

import java.util.Locale;

import com.google.common.base.Optional;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;

final class ScriptBlockStateParser {
    private ScriptBlockStateParser() {
    }

    static IBlockState parse(String value) {
        String source = value == null ? "" : value.trim();
        int bracket = source.indexOf('[');
        String idText = bracket < 0 ? source : source.substring(0, bracket);
        Block block = Block.REGISTRY.getObject(new ResourceLocation(idText));
        if (block == null || block == Blocks.AIR && !"minecraft:air".equals(idText))
            throw new IllegalArgumentException("Unknown block state id: " + idText);
        IBlockState state = block.getDefaultState();
        if (bracket < 0) return state;
        if (!source.endsWith("]")) throw new IllegalArgumentException("Malformed block state: " + value);
        String properties = source.substring(bracket + 1, source.length() - 1).trim();
        if (properties.isEmpty()) return state;
        for (String assignment : properties.split(",")) {
            String[] pair = assignment.split("=", 2);
            if (pair.length != 2) throw new IllegalArgumentException("Malformed block property: " + assignment);
            IProperty<?> property = findProperty(state, pair[0].trim());
            if (property == null) throw new IllegalArgumentException("Unknown property " + pair[0] + " for " + idText);
            state = withValue(state, property, pair[1].trim().toLowerCase(Locale.ROOT));
        }
        return state;
    }

    private static IProperty<?> findProperty(IBlockState state, String name) {
        for (IProperty<?> property : state.getPropertyKeys())
            if (property.getName().equals(name)) return property;
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static IBlockState withValue(IBlockState state, IProperty property, String value) {
        Optional parsed = property.parseValue(value);
        if (!parsed.isPresent()) throw new IllegalArgumentException("Invalid value " + value + " for " + property.getName());
        return state.withProperty(property, (Comparable) parsed.get());
    }
}
