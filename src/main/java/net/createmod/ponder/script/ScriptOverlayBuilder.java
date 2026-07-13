package net.createmod.ponder.script;

import crafttweaker.annotations.ZenRegister;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;
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
    public void showText(int duration, String text, ScriptVector target, String color,
                         boolean nearTarget, boolean keyframe) {
        ScriptVector required = ScriptVector.require(target, "Text target");
        showText(duration, text, required.x, required.y, required.z, color, nearTarget, keyframe);
    }

    @ZenMethod
    public void showSharedText(int duration, String key, String[] params, double x, double y, double z,
                               String color, boolean nearTarget, boolean keyframe) {
        NBTTagCompound data = ScriptWorldBuilder.vector(x, y, z);
        data.setInteger("duration", duration(duration));
        data.setString("key", sharedKey(key).toString());
        NBTTagList values = new NBTTagList();
        if (params != null) {
            for (String param : params)
                values.appendTag(new NBTTagString(requiredParameter(param)));
        }
        data.setTag("params", values);
        data.setString("color", palette(color));
        data.setBoolean("near", nearTarget);
        data.setBoolean("keyframe", keyframe);
        scene.add("show_shared_text", data);
    }

    @ZenMethod
    public void showSharedText(int duration, String key, String[] params, ScriptVector target,
                               String color, boolean nearTarget, boolean keyframe) {
        ScriptVector required = ScriptVector.require(target, "Shared text target");
        showSharedText(duration, key, params, required.x, required.y, required.z,
            color, nearTarget, keyframe);
    }

    @ZenMethod
    public void showIndependentText(int duration, String text, int y, String color, boolean keyframe) {
        NBTTagCompound data = new NBTTagCompound();
        data.setInteger("duration", duration(duration));
        data.setString("text", ScriptWorldBuilder.requiredText(text, "text"));
        data.setInteger("y", y);
        data.setString("color", palette(color));
        data.setBoolean("keyframe", keyframe);
        scene.add("show_independent_text", data);
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
    public void showControls(int duration, ScriptVector target, String pointing, String action, String itemId) {
        ScriptVector required = ScriptVector.require(target, "Input target");
        showControls(duration, required.x, required.y, required.z, pointing, action, itemId);
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
    public void showLine(String color, ScriptVector from, ScriptVector to, int duration, boolean big) {
        ScriptVector requiredFrom = ScriptVector.require(from, "Line start");
        ScriptVector requiredTo = ScriptVector.require(to, "Line end");
        showLine(color, requiredFrom.x, requiredFrom.y, requiredFrom.z,
            requiredTo.x, requiredTo.y, requiredTo.z, duration, big);
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

    @ZenMethod
    public void showOutlineWithText(String text, String color, ScriptSelection selection,
                                    int duration, boolean keyframe) {
        if (selection == null) throw new IllegalArgumentException("Selection is required");
        NBTTagCompound data = new NBTTagCompound();
        data.setString("text", ScriptWorldBuilder.requiredText(text, "outline text"));
        data.setString("color", palette(color));
        data.setTag("selection", selection.serialize());
        data.setInteger("duration", duration(duration));
        data.setBoolean("keyframe", keyframe);
        scene.add("show_outline_text", data);
    }

    @ZenMethod
    public void showBoundingBox(String color, String slot, double minX, double minY, double minZ,
                                double maxX, double maxY, double maxZ, int duration) {
        ScriptVector.validate(minX, minY, minZ, "Bounding box minimum");
        ScriptVector.validate(maxX, maxY, maxZ, "Bounding box maximum");
        NBTTagCompound data = new NBTTagCompound();
        data.setString("color", palette(color));
        data.setString("slot", ScriptWorldBuilder.requiredText(
            slot == null || slot.isEmpty() ? "default" : slot, "bounding box slot"));
        data.setDouble("minX", minX); data.setDouble("minY", minY); data.setDouble("minZ", minZ);
        data.setDouble("maxX", maxX); data.setDouble("maxY", maxY); data.setDouble("maxZ", maxZ);
        data.setInteger("duration", duration(duration));
        scene.add("show_bounding_box", data);
    }

    @ZenMethod
    public void showBoundingBox(String color, String slot, ScriptVector minimum,
                                ScriptVector maximum, int duration) {
        ScriptVector min = ScriptVector.require(minimum, "Bounding box minimum");
        ScriptVector max = ScriptVector.require(maximum, "Bounding box maximum");
        showBoundingBox(color, slot, min.x, min.y, min.z, max.x, max.y, max.z, duration);
    }

    @ZenMethod
    public void showScrollInput(ScriptVector location, String side, int duration) {
        ScriptVector required = ScriptVector.require(location, "Scroll input location");
        NBTTagCompound data = ScriptWorldBuilder.vector(required.x, required.y, required.z);
        data.setString("side", ScriptWorldBuilder.direction(side));
        data.setInteger("duration", duration(duration));
        scene.add("show_scroll_input", data);
    }

    @ZenMethod
    public void showCenteredScrollInput(ScriptPosition position, String side, int duration) {
        ScriptPosition required = ScriptPosition.require(position, "Centered scroll input position");
        NBTTagCompound data = ScriptWorldBuilder.position(required.x, required.y, required.z);
        data.setString("side", ScriptWorldBuilder.direction(side));
        data.setInteger("duration", duration(duration));
        scene.add("show_centered_scroll_input", data);
    }

    @ZenMethod
    public void showRepeaterScrollInput(ScriptPosition position, int duration) {
        ScriptPosition required = ScriptPosition.require(position, "Repeater input position");
        NBTTagCompound data = ScriptWorldBuilder.position(required.x, required.y, required.z);
        data.setInteger("duration", duration(duration));
        scene.add("show_repeater_scroll_input", data);
    }

    @ZenMethod
    public void showFilterSlotInput(ScriptVector location, int duration) {
        showFilterSlotInput(location, "down", duration);
    }

    @ZenMethod
    public void showFilterSlotInput(ScriptVector location, String side, int duration) {
        ScriptVector required = ScriptVector.require(location, "Filter slot input location");
        NBTTagCompound data = ScriptWorldBuilder.vector(required.x, required.y, required.z);
        data.setString("side", ScriptWorldBuilder.direction(side));
        data.setInteger("duration", duration(duration));
        scene.add("show_filter_slot_input", data);
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

    private ResourceLocation sharedKey(String value) {
        String key = ScriptWorldBuilder.requiredText(value, "shared text key");
        return key.indexOf(':') >= 0
            ? ScriptSceneRegistry.parseId(key, "shared text key")
            : new ResourceLocation(scene.getSceneId().getNamespace(), key);
    }

    private static String requiredParameter(String value) {
        if (value == null) throw new IllegalArgumentException("Shared text parameters may not be null");
        if (value.length() > 8192)
            throw new IllegalArgumentException("Shared text parameter exceeds 8192 characters");
        return value;
    }
}
