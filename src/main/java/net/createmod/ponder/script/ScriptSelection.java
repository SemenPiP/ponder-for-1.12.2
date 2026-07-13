package net.createmod.ponder.script;

import crafttweaker.annotations.ZenRegister;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.ponder.Selection")
public final class ScriptSelection {
    private final String type;
    private final int[] values;

    private ScriptSelection(String type, int... values) {
        this.type = type;
        this.values = values.clone();
    }

    @ZenMethod
    public static ScriptSelection position(int x, int y, int z) {
        return new ScriptSelection("position", x, y, z);
    }

    @ZenMethod
    public static ScriptSelection fromTo(int x1, int y1, int z1, int x2, int y2, int z2) {
        return new ScriptSelection("from_to", x1, y1, z1, x2, y2, z2);
    }

    @ZenMethod
    public static ScriptSelection layer(int y) {
        return new ScriptSelection("layer", y);
    }

    @ZenMethod
    public static ScriptSelection layersFrom(int y) {
        return new ScriptSelection("layers_from", y);
    }

    @ZenMethod
    public static ScriptSelection everywhere() {
        return new ScriptSelection("everywhere");
    }

    NBTTagCompound serialize() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("type", type);
        tag.setIntArray("values", values);
        return tag;
    }

    static ScriptSelection deserialize(NBTTagCompound tag) {
        return new ScriptSelection(tag.getString("type"), tag.getIntArray("values"));
    }

    Selection resolve(SceneBuildingUtil util) {
        if ("position".equals(type))
            return util.select().position(new BlockPos(values[0], values[1], values[2]));
        if ("from_to".equals(type))
            return util.select().fromTo(values[0], values[1], values[2], values[3], values[4], values[5]);
        if ("layer".equals(type))
            return util.select().layer(values[0]);
        if ("layers_from".equals(type))
            return util.select().layersFrom(values[0]);
        if ("everywhere".equals(type))
            return util.select().everywhere();
        throw new IllegalArgumentException("Unknown script selection type: " + type);
    }
}
