package net.createmod.ponder.script;

import crafttweaker.annotations.ZenRegister;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.minecraft.nbt.NBTTagCompound;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.ponder.Overlay")
public final class ScriptOverlayBuilder {
    private final ScriptSceneBuilder scene;

    ScriptOverlayBuilder(ScriptSceneBuilder scene) {
        this.scene = scene;
    }

    @ZenMethod
    public void showText(int duration, String text, double x, double y, double z, String color,
                         boolean nearTarget, boolean keyframe) {
        NBTTagCompound data = ScriptWorldBuilder.vector(x, y, z);
        data.setInteger("duration", duration(duration));
        data.setString("text", ScriptWorldBuilder.requiredText(text, "text"));
        data.setString("color", palette(color));
        data.setBoolean("near", nearTarget);
        data.setBoolean("keyframe", keyframe);
        scene.add("show_text", data);
    }

    @ZenMethod
    public void showControls(int duration, double x, double y, double z, String pointing, String action,
                             String itemId) {
        NBTTagCompound data = ScriptWorldBuilder.vector(x, y, z);
        data.setInteger("duration", duration(duration));
        data.setString("pointing", pointing(pointing));
        data.setString("action", action(action));
        if (itemId != null && !itemId.isEmpty()) {
            data.setString("item", ScriptSceneRegistry.parseId(itemId, "control item id").toString());
        }
        scene.add("show_controls", data);
    }

    @ZenMethod
    public void showLine(String color, double x1, double y1, double z1, double x2, double y2, double z2,
                         int duration, boolean big) {
        NBTTagCompound data = new NBTTagCompound();
        data.setString("color", palette(color));
        data.setDouble("x1", x1); data.setDouble("y1", y1); data.setDouble("z1", z1);
        data.setDouble("x2", x2); data.setDouble("y2", y2); data.setDouble("z2", z2);
        data.setInteger("duration", duration(duration)); data.setBoolean("big", big);
        scene.add("show_line", data);
    }

    @ZenMethod
    public void showOutline(String color, String slot, ScriptSelection selection, int duration) {
        if (selection == null) throw new IllegalArgumentException("Selection is required");
        NBTTagCompound data = new NBTTagCompound();
        data.setString("color", palette(color));
        data.setString("slot", ScriptWorldBuilder.requiredText(
            slot == null || slot.isEmpty() ? "default" : slot, "outline slot"));
        data.setTag("selection", selection.serialize());
        data.setInteger("duration", duration(duration));
        scene.add("show_outline", data);
    }

    private static int duration(int value) {
        if (value < 0 || value > 72000) throw new IllegalArgumentException("Duration must be 0..72000");
        return value;
    }

    private static String palette(String value) {
        String normalized = value == null || value.isEmpty() ? "white" : value.toLowerCase(java.util.Locale.ROOT);
        PonderPalette.valueOf(normalized.toUpperCase(java.util.Locale.ROOT));
        return normalized;
    }

    private static String pointing(String value) {
        String normalized = value == null || value.isEmpty() ? "down" : value.toLowerCase(java.util.Locale.ROOT);
        Pointing.valueOf(normalized.toUpperCase(java.util.Locale.ROOT));
        return normalized;
    }

    private static String action(String value) {
        String normalized = value == null || value.isEmpty()
            ? "right_click" : value.toLowerCase(java.util.Locale.ROOT);
        if (!normalized.equals("right_click") && !normalized.equals("left_click") && !normalized.equals("scroll"))
            throw new IllegalArgumentException("Unknown control action: " + value);
        return normalized;
    }
}
