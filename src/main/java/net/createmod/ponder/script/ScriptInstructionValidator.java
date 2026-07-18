package net.createmod.ponder.script;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.script.ScriptInstructionCodec;
import net.createmod.ponder.api.script.ScriptInstructionCodecs;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;

/** Shared structural validation for local builders and untrusted network IR. */
public final class ScriptInstructionValidator {
    private static final int MAX_TEXT = 8192;
    private static final int MAX_RESOURCE_ID = 256;
    private static final int MAX_DURATION = 72000;

    private ScriptInstructionValidator() {
    }

    public static void validate(ResourceLocation sceneId, List<ScriptInstruction> instructions) {
        if (sceneId == null || instructions == null)
            throw new IllegalArgumentException("Scene id and instructions are required");
        Map<String, ScriptSceneBuilder.HandleType> handles =
            new LinkedHashMap<String, ScriptSceneBuilder.HandleType>();
        for (int index = 0; index < instructions.size(); index++) {
            ScriptInstruction instruction = instructions.get(index);
            if (instruction == null)
                throw failure(sceneId, index, "<null>", "Instruction is required", null);
            try {
                validateInstruction(instruction, handles);
            } catch (RuntimeException exception) {
                throw failure(sceneId, index, instruction.getOperation(), exception.getMessage(), exception);
            }
        }
    }

    private static void validateInstruction(ScriptInstruction instruction,
                                            Map<String, ScriptSceneBuilder.HandleType> handles) {
        String op = instruction.getOperation();
        NBTTagCompound data = instruction.getData();
        if ("configure_base_plate".equals(op)) {
            requireInt(data, "x");
            requireInt(data, "z");
            range(requireInt(data, "size"), 1, 256, "Base plate size");
        } else if (isEmptyOperation(op)) {
            return;
        } else if ("scale".equals(op)) {
            double value = requireFloat(data, "value");
            if (!(value > 0) || value > 16)
                throw new IllegalArgumentException("Scene scale must be > 0 and <= 16");
        } else if ("offset_y".equals(op)) {
            requireFloat(data, "value");
        } else if ("idle".equals(op)) {
            range(requireInt(data, "ticks"), 0, MAX_DURATION, "Idle ticks");
        } else if ("rotate_camera".equals(op)) {
            requireFloat(data, "degrees");
        } else if ("next_up".equals(op)) {
            requireBoolean(data, "enabled");
        } else if ("show_section".equals(op) || "hide_section".equals(op)) {
            requireSelection(data);
            requireDirection(data, "direction");
        } else if ("restore_blocks".equals(op) || "toggle_redstone".equals(op)) {
            requireSelection(data);
        } else if ("show_independent".equals(op)) {
            define(handles, requireHandle(data), ScriptSceneBuilder.HandleType.SECTION);
            requireSelection(data);
            requireDirection(data, "direction");
        } else if ("show_independent_immediate".equals(op) || "make_independent".equals(op)) {
            define(handles, requireHandle(data), ScriptSceneBuilder.HandleType.SECTION);
            requireSelection(data);
        } else if ("show_section_merge".equals(op)) {
            require(handles, requireHandle(data), ScriptSceneBuilder.HandleType.SECTION);
            requireSelection(data);
            requireDirection(data, "direction");
        } else if ("glue_block".equals(op)) {
            require(handles, requireHandle(data), ScriptSceneBuilder.HandleType.SECTION);
            requirePosition(data);
            requireDirection(data, "direction");
        } else if ("hide_independent".equals(op)) {
            require(handles, requireHandle(data), ScriptSceneBuilder.HandleType.SECTION);
            requireDirection(data, "direction");
        } else if ("move_section".equals(op) || "rotate_section".equals(op)) {
            require(handles, requireHandle(data), ScriptSceneBuilder.HandleType.SECTION);
            requireVector(data);
            requireDuration(data);
        } else if ("center_section".equals(op) || "stabilize_section".equals(op)) {
            require(handles, requireHandle(data), ScriptSceneBuilder.HandleType.SECTION);
            requireVector(data);
        } else if ("set_block".equals(op)) {
            requirePosition(data);
            requireString(data, "state", MAX_TEXT, false);
            requireBoolean(data, "particles");
        } else if ("set_blocks".equals(op)) {
            requireSelection(data);
            requireString(data, "state", MAX_TEXT, false);
            requireBoolean(data, "particles");
        } else if ("destroy_block".equals(op) || "break_progress".equals(op)
            || "indicate_redstone".equals(op) || "indicate_success".equals(op)) {
            requirePosition(data);
        } else if ("cycle_property".equals(op)) {
            requirePosition(data);
            requireString(data, "property", MAX_TEXT, false);
        } else if ("create_item".equals(op)) {
            define(handles, requireHandle(data), ScriptSceneBuilder.HandleType.ITEM);
            requireVector(data);
            requireFiniteDouble(data, "mx");
            requireFiniteDouble(data, "my");
            requireFiniteDouble(data, "mz");
            requireResource(data, "item");
            range(requireInt(data, "count"), 1, 64, "Item count");
            requireInt(data, "meta");
        } else if ("move_item".equals(op)) {
            require(handles, requireHandle(data), ScriptSceneBuilder.HandleType.ITEM);
            requireVector(data);
            requireDuration(data);
        } else if ("set_item_visible".equals(op)) {
            require(handles, requireHandle(data), ScriptSceneBuilder.HandleType.ITEM);
            requireBoolean(data, "visible");
        } else if ("remove_item".equals(op)) {
            terminate(handles, requireHandle(data), ScriptSceneBuilder.HandleType.ITEM);
        } else if ("create_minecart".equals(op)) {
            define(handles, requireHandle(data), ScriptSceneBuilder.HandleType.MINECART);
            requireVector(data);
            requireFloat(data, "angle");
            ScriptWorldBuilder.minecartType(requireString(data, "type", MAX_TEXT, false));
        } else if ("move_minecart".equals(op)) {
            require(handles, requireHandle(data), ScriptSceneBuilder.HandleType.MINECART);
            requireVector(data);
            requireDuration(data);
        } else if ("rotate_minecart".equals(op)) {
            require(handles, requireHandle(data), ScriptSceneBuilder.HandleType.MINECART);
            requireFloat(data, "angle");
            requireDuration(data);
        } else if ("hide_minecart".equals(op)) {
            require(handles, requireHandle(data), ScriptSceneBuilder.HandleType.MINECART);
            requireDirection(data, "direction");
        } else if ("create_parrot".equals(op)) {
            define(handles, requireHandle(data), ScriptSceneBuilder.HandleType.PARROT);
            requireVector(data);
            ScriptWorldBuilder.parrotPose(requireString(data, "pose", MAX_TEXT, false));
        } else if ("change_parrot_pose".equals(op)) {
            require(handles, requireHandle(data), ScriptSceneBuilder.HandleType.PARROT);
            ScriptWorldBuilder.parrotPose(requireString(data, "pose", MAX_TEXT, false));
        } else if ("move_parrot".equals(op) || "rotate_parrot".equals(op)) {
            require(handles, requireHandle(data), ScriptSceneBuilder.HandleType.PARROT);
            requireVector(data);
            requireDuration(data);
        } else if ("hide_parrot".equals(op)) {
            require(handles, requireHandle(data), ScriptSceneBuilder.HandleType.PARROT);
            requireDirection(data, "direction");
        } else if ("tile_nbt".equals(op)) {
            requireSelection(data);
            NBTTagCompound nbt = requireCompound(data, "nbt");
            requireBoolean(data, "replace");
            requireBoolean(data, "redraw");
            requireNbtLimit(nbt, "Tile NBT");
        } else if ("show_text".equals(op)) {
            requireTextPresentation(data, true);
            requireVector(data);
            requireBoolean(data, "near");
        } else if ("show_shared_text".equals(op)) {
            requireTextPresentation(data, false);
            requireResource(data, "key");
            requireStringList(data, "params");
            requireVector(data);
            requireBoolean(data, "near");
        } else if ("show_independent_text".equals(op)) {
            requireTextPresentation(data, true);
            requireInt(data, "y");
        } else if ("show_outline_text".equals(op)) {
            requireTextPresentation(data, true);
            requireSelection(data);
        } else if ("show_controls".equals(op)) {
            requireVector(data);
            requireDuration(data);
            requirePointing(data, "pointing");
            String action = requireString(data, "action", MAX_TEXT, false);
            if (!"right_click".equals(action) && !"left_click".equals(action) && !"scroll".equals(action))
                throw new IllegalArgumentException("Unknown control action: " + action);
            if (data.hasKey("item"))
                requireResource(data, "item");
        } else if ("show_line".equals(op)) {
            requirePalette(data, "color");
            requireFiniteDouble(data, "x1");
            requireFiniteDouble(data, "y1");
            requireFiniteDouble(data, "z1");
            requireFiniteDouble(data, "x2");
            requireFiniteDouble(data, "y2");
            requireFiniteDouble(data, "z2");
            requireDuration(data);
            requireBoolean(data, "big");
        } else if ("show_outline".equals(op)) {
            requirePalette(data, "color");
            requireString(data, "slot", MAX_TEXT, false);
            requireSelection(data);
            requireDuration(data);
        } else if ("show_bounding_box".equals(op)) {
            requirePalette(data, "color");
            requireString(data, "slot", MAX_TEXT, false);
            requireFiniteDouble(data, "minX");
            requireFiniteDouble(data, "minY");
            requireFiniteDouble(data, "minZ");
            requireFiniteDouble(data, "maxX");
            requireFiniteDouble(data, "maxY");
            requireFiniteDouble(data, "maxZ");
            requireDuration(data);
        } else if ("show_scroll_input".equals(op) || "show_filter_slot_input".equals(op)) {
            requireVector(data);
            requireDirection(data, "side");
            requireDuration(data);
        } else if ("show_centered_scroll_input".equals(op)) {
            requirePosition(data);
            requireDirection(data, "side");
            requireDuration(data);
        } else if ("show_repeater_scroll_input".equals(op)) {
            requirePosition(data);
            requireDuration(data);
        } else if ("redstone_particles".equals(op)) {
            requirePosition(data);
            requireInt(data, "color");
            range(requireInt(data, "amount"), 0, 4096, "Particle amount");
        } else if ("particles".equals(op) || "particles_within_block".equals(op)) {
            requireVector(data);
            requireParticle(data, "type");
            requireFiniteDouble(data, "mx");
            requireFiniteDouble(data, "my");
            requireFiniteDouble(data, "mz");
            double amount = requireFloat(data, "amount");
            if (amount < 0 || amount > 4096)
                throw new IllegalArgumentException("Particle amount must be 0..4096");
            range(requireInt(data, "cycles"), 0, MAX_DURATION, "Particle cycles");
        } else if ("move_poi".equals(op)) {
            requireVector(data);
        } else if ("custom".equals(op)) {
            ResourceLocation codecId = requireResource(data, "codec");
            NBTTagCompound payload = requireCompound(data, "payload");
            requireNbtLimit(payload, "Custom instruction payload");
            ScriptInstructionCodec codec = ScriptInstructionCodecs.get(codecId);
            if (codec == null)
                throw new IllegalArgumentException("Missing custom instruction codec " + codecId);
            codec.validate(payload.copy());
            ScriptCodecDescriptors.requirement(codec, payload);
        } else {
            throw new IllegalArgumentException("Unknown Ponder script instruction " + op);
        }
    }

    private static boolean isEmptyOperation(String op) {
        return "show_base_plate".equals(op) || "remove_shadow".equals(op) || "keyframe".equals(op)
            || "lazy_keyframe".equals(op) || "finish".equals(op);
    }

    private static void requireTextPresentation(NBTTagCompound data, boolean literal) {
        requireDuration(data);
        if (literal)
            requireString(data, "text", MAX_TEXT, false);
        requirePalette(data, "color");
        requireBoolean(data, "keyframe");
    }

    private static void requirePosition(NBTTagCompound data) {
        requireInt(data, "x");
        requireInt(data, "y");
        requireInt(data, "z");
    }

    private static void requireVector(NBTTagCompound data) {
        requireFiniteDouble(data, "x");
        requireFiniteDouble(data, "y");
        requireFiniteDouble(data, "z");
    }

    private static void requireDuration(NBTTagCompound data) {
        range(requireInt(data, "duration"), 0, MAX_DURATION, "Duration");
    }

    private static void requireSelection(NBTTagCompound data) {
        NBTTagCompound selection = requireCompound(data, "selection");
        String type = requireString(selection, "type", MAX_TEXT, false);
        if (!selection.hasKey("values", 11))
            throw new IllegalArgumentException("Selection values must be an int array");
        int[] values = selection.getIntArray("values");
        int expected;
        if ("position".equals(type))
            expected = 3;
        else if ("from_to".equals(type) || "cuboid".equals(type))
            expected = 6;
        else if ("column".equals(type))
            expected = 2;
        else if ("layer".equals(type) || "layers_from".equals(type))
            expected = 1;
        else if ("layers".equals(type))
            expected = 2;
        else if ("everywhere".equals(type))
            expected = 0;
        else
            throw new IllegalArgumentException("Unknown script selection type: " + type);
        if (values.length != expected)
            throw new IllegalArgumentException("Selection " + type + " requires " + expected + " values");
        if ("layers".equals(type) && values[1] <= 0)
            throw new IllegalArgumentException("Selection layers height must be positive");
    }

    private static String requireHandle(NBTTagCompound data) {
        String handle = requireString(data, "handle", 64, false);
        ScriptSceneBuilder.validateHandle(handle);
        return handle;
    }

    private static void define(Map<String, ScriptSceneBuilder.HandleType> handles, String handle,
                               ScriptSceneBuilder.HandleType type) {
        ScriptSceneBuilder.HandleType previous = handles.get(handle);
        if (previous != null)
            throw new IllegalArgumentException(previous == ScriptSceneBuilder.HandleType.TERMINATED
                ? "Scene handle is terminated and may not be reused: " + handle
                : "Duplicate scene handle: " + handle);
        handles.put(handle, type);
    }

    private static void require(Map<String, ScriptSceneBuilder.HandleType> handles, String handle,
                                ScriptSceneBuilder.HandleType expected) {
        ScriptSceneBuilder.HandleType actual = handles.get(handle);
        if (actual == null)
            throw new IllegalArgumentException("Unknown scene handle: " + handle);
        if (actual == ScriptSceneBuilder.HandleType.TERMINATED)
            throw new IllegalArgumentException("Scene handle is terminated: " + handle);
        if (actual != expected)
            throw new IllegalArgumentException("Scene handle '" + handle + "' is " + actual.scriptName
                + ", expected " + expected.scriptName);
    }

    private static void terminate(Map<String, ScriptSceneBuilder.HandleType> handles, String handle,
                                  ScriptSceneBuilder.HandleType expected) {
        require(handles, handle, expected);
        handles.put(handle, ScriptSceneBuilder.HandleType.TERMINATED);
    }

    private static String requireDirection(NBTTagCompound data, String key) {
        String value = requireString(data, key, MAX_TEXT, false);
        EnumFacing.valueOf(value.toUpperCase(Locale.ROOT));
        return value;
    }

    private static void requirePalette(NBTTagCompound data, String key) {
        PonderPalette.valueOf(requireString(data, key, MAX_TEXT, false).toUpperCase(Locale.ROOT));
    }

    private static void requirePointing(NBTTagCompound data, String key) {
        Pointing.valueOf(requireString(data, key, MAX_TEXT, false).toUpperCase(Locale.ROOT));
    }

    private static void requireParticle(NBTTagCompound data, String key) {
        EnumParticleTypes.valueOf(requireString(data, key, MAX_TEXT, false).toUpperCase(Locale.ROOT));
    }

    private static ResourceLocation requireResource(NBTTagCompound data, String key) {
        String value = requireString(data, key, MAX_RESOURCE_ID, false);
        try {
            return new ResourceLocation(value);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid resource id in " + key + ": " + value, exception);
        }
    }

    private static NBTTagCompound requireCompound(NBTTagCompound data, String key) {
        if (!data.hasKey(key, 10))
            throw new IllegalArgumentException("Instruction field '" + key + "' must be a compound");
        return data.getCompoundTag(key);
    }

    private static String requireString(NBTTagCompound data, String key, int maxLength, boolean allowEmpty) {
        if (!data.hasKey(key, 8))
            throw new IllegalArgumentException("Instruction field '" + key + "' must be a string");
        String value = data.getString(key);
        if (!allowEmpty && value.trim().isEmpty())
            throw new IllegalArgumentException("Instruction field '" + key + "' may not be empty");
        if (value.length() > maxLength)
            throw new IllegalArgumentException("Instruction field '" + key + "' exceeds " + maxLength + " characters");
        return value;
    }

    private static int requireInt(NBTTagCompound data, String key) {
        if (!data.hasKey(key, 3))
            throw new IllegalArgumentException("Instruction field '" + key + "' must be an integer");
        return data.getInteger(key);
    }

    private static double requireFloat(NBTTagCompound data, String key) {
        if (!data.hasKey(key, 5))
            throw new IllegalArgumentException("Instruction field '" + key + "' must be a float");
        double value = data.getFloat(key);
        if (!Double.isFinite(value))
            throw new IllegalArgumentException("Instruction field '" + key + "' must be finite");
        return value;
    }

    private static double requireFiniteDouble(NBTTagCompound data, String key) {
        if (!data.hasKey(key, 6))
            throw new IllegalArgumentException("Instruction field '" + key + "' must be a double");
        double value = data.getDouble(key);
        if (!Double.isFinite(value))
            throw new IllegalArgumentException("Instruction field '" + key + "' must be finite");
        return value;
    }

    private static boolean requireBoolean(NBTTagCompound data, String key) {
        if (!data.hasKey(key, 1))
            throw new IllegalArgumentException("Instruction field '" + key + "' must be a boolean");
        return data.getBoolean(key);
    }

    private static void requireStringList(NBTTagCompound data, String key) {
        if (!data.hasKey(key, 9))
            throw new IllegalArgumentException("Instruction field '" + key + "' must be a list");
        NBTTagList list = data.getTagList(key, 8);
        for (int i = 0; i < list.tagCount(); i++) {
            String value = list.getStringTagAt(i);
            if (value.length() > MAX_TEXT)
                throw new IllegalArgumentException("Instruction field '" + key + "' entry exceeds "
                    + MAX_TEXT + " characters");
        }
    }

    private static void requireNbtLimit(NBTTagCompound data, String label) {
        try {
            if (ScriptSceneSnapshot.uncompressedSize(data) > ScriptInstruction.MAX_DATA_BYTES)
                throw new IllegalArgumentException(label + " exceeds " + ScriptInstruction.MAX_DATA_BYTES + " bytes");
        } catch (IOException exception) {
            throw new IllegalArgumentException("Could not measure " + label, exception);
        }
    }

    private static void range(int value, int min, int max, String label) {
        if (value < min || value > max)
            throw new IllegalArgumentException(label + " must be " + min + ".." + max);
    }

    private static IllegalArgumentException failure(ResourceLocation sceneId, int index, String operation,
                                                    String reason, RuntimeException cause) {
        String message = "Ponder script " + sceneId + " instruction #" + index + " (" + operation
            + ") is invalid: " + (reason == null ? "unknown validation error" : reason);
        return cause == null ? new IllegalArgumentException(message) : new IllegalArgumentException(message, cause);
    }
}
