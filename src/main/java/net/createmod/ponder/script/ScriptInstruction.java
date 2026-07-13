package net.createmod.ponder.script;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;

public final class ScriptInstruction {
    public static final int MAX_DATA_BYTES = 256 * 1024;
    private static final Set<String> BUILTIN_OPERATIONS = Collections.unmodifiableSet(new HashSet<String>(
        Arrays.asList(
            "configure_base_plate", "show_base_plate", "remove_shadow", "scale", "offset_y", "idle",
            "rotate_camera", "keyframe", "lazy_keyframe", "finish", "next_up", "show_section", "hide_section",
            "restore_blocks", "show_independent", "show_independent_immediate", "hide_independent",
            "make_independent", "show_section_merge", "glue_block", "move_section", "rotate_section",
            "center_section", "stabilize_section", "set_block", "set_blocks", "destroy_block", "break_progress",
            "cycle_property", "toggle_redstone", "create_item", "create_minecart", "move_minecart",
            "rotate_minecart", "create_parrot", "change_parrot_pose", "move_parrot", "rotate_parrot", "tile_nbt",
            "show_text", "show_controls", "show_line", "show_outline", "indicate_redstone", "indicate_success",
            "redstone_particles", "particles", "particles_within_block", "move_poi", "hide_minecart",
            "hide_parrot", "custom"
        )));
    private final String operation;
    private final NBTTagCompound data;

    public ScriptInstruction(String operation, NBTTagCompound data) {
        if (operation == null || operation.trim().isEmpty())
            throw new IllegalArgumentException("Script instruction operation is required");
        if (!BUILTIN_OPERATIONS.contains(operation))
            throw new IllegalArgumentException("Unknown Ponder script instruction operation: " + operation);
        this.operation = operation;
        this.data = data == null ? new NBTTagCompound() : data.copy();
        try {
            if (ScriptSceneSnapshot.uncompressedSize(this.data) > MAX_DATA_BYTES)
                throw new IllegalArgumentException("Script instruction data exceeds " + MAX_DATA_BYTES + " bytes");
        } catch (IOException exception) {
            throw new IllegalArgumentException("Could not measure script instruction data", exception);
        }
    }

    public String getOperation() {
        return operation;
    }

    public NBTTagCompound getData() {
        return data.copy();
    }

    public NBTTagCompound serialize() {
        NBTTagCompound result = new NBTTagCompound();
        result.setString("op", operation);
        result.setTag("data", data.copy());
        return result;
    }

    public static ScriptInstruction deserialize(NBTTagCompound tag) {
        return new ScriptInstruction(tag.getString("op"), tag.getCompoundTag("data"));
    }
}
