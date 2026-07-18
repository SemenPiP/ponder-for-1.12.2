package net.createmod.ponder.script;

import crafttweaker.annotations.ZenRegister;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.ponder.Selection")
public final class ScriptSelection {
    private static final String STRUCTURE_GROUP_FIELD = "structure_group";
    private final String type;
    private final String groupName;
    private final int[] values;

    private ScriptSelection(String type, int... values) {
        this(type, null, values);
    }

    private ScriptSelection(String type, String groupName, int... values) {
        this.type = type;
        this.groupName = groupName;
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
    public static ScriptSelection column(int x, int z) {
        return new ScriptSelection("column", x, z);
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
    public static ScriptSelection layers(int y, int height) {
        if (height <= 0)
            throw new IllegalArgumentException("Layer height must be greater than zero");
        return new ScriptSelection("layers", y, height);
    }

    @ZenMethod
    public static ScriptSelection cuboid(int x, int y, int z, int offsetX, int offsetY, int offsetZ) {
        return new ScriptSelection("cuboid", x, y, z, offsetX, offsetY, offsetZ);
    }

    @ZenMethod
    public static ScriptSelection everywhere() {
        return new ScriptSelection("everywhere");
    }

    @ZenMethod
    public static ScriptSelection structureGroup(String name) {
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("Structure group name is required");
        if (name.length() > 256)
            throw new IllegalArgumentException("Structure group name may not exceed 256 characters");
        return new ScriptSelection("structure_group", name);
    }

    NBTTagCompound serialize() {
        NBTTagCompound tag = new NBTTagCompound();
        // The 1.1.1 validator accepts this envelope and preserves extension fields.
        tag.setString("type", "structure_group".equals(type) ? "everywhere" : type);
        tag.setIntArray("values", values);
        if ("structure_group".equals(type))
            tag.setString(STRUCTURE_GROUP_FIELD, groupName);
        return tag;
    }

    static ScriptSelection deserialize(NBTTagCompound tag) {
        if (tag.hasKey(STRUCTURE_GROUP_FIELD, 8))
            return structureGroup(tag.getString(STRUCTURE_GROUP_FIELD));
        return new ScriptSelection(tag.getString("type"), tag.getIntArray("values"));
    }

    Selection resolve(SceneBuildingUtil util) {
        if ("position".equals(type))
            return util.select().position(new BlockPos(values[0], values[1], values[2]));
        if ("from_to".equals(type))
            return util.select().fromTo(values[0], values[1], values[2], values[3], values[4], values[5]);
        if ("column".equals(type))
            return util.select().column(values[0], values[1]);
        if ("layer".equals(type))
            return util.select().layer(values[0]);
        if ("layers_from".equals(type))
            return util.select().layersFrom(values[0]);
        if ("layers".equals(type))
            return util.select().layers(values[0], values[1]);
        if ("cuboid".equals(type))
            return util.select().cuboid(new BlockPos(values[0], values[1], values[2]),
                new Vec3i(values[3], values[4], values[5]));
        if ("everywhere".equals(type))
            return util.select().everywhere();
        if ("structure_group".equals(type))
            return util.select().structureGroup(groupName);
        throw new IllegalArgumentException("Unknown script selection type: " + type);
    }
}
