package net.createmod.ponder.script;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.data.IData;
import crafttweaker.mc1120.data.NBTConverter;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.ponder.World")
public final class ScriptWorldBuilder {
    private final ScriptSceneBuilder scene;

    ScriptWorldBuilder(ScriptSceneBuilder scene) {
        this.scene = scene;
    }

    @ZenMethod
    public void showSection(ScriptSelection selection, String direction) {
        scene.add("show_section", selection(selection, direction));
    }

    @ZenMethod
    public void hideSection(ScriptSelection selection, String direction) {
        scene.add("hide_section", selection(selection, direction));
    }

    @ZenMethod
    public void restoreBlocks(ScriptSelection selection) {
        NBTTagCompound data = new NBTTagCompound();
        data.setTag("selection", required(selection).serialize());
        scene.add("restore_blocks", data);
    }

    @ZenMethod
    public void showIndependentSection(String handle, ScriptSelection selection, String direction) {
        scene.defineHandle(handle);
        NBTTagCompound data = selection(selection, direction);
        data.setString("handle", handle);
        scene.add("show_independent", data);
    }

    @ZenMethod
    public void showIndependentSectionImmediately(String handle, ScriptSelection selection) {
        scene.defineHandle(handle);
        NBTTagCompound data = new NBTTagCompound();
        data.setString("handle", handle);
        data.setTag("selection", required(selection).serialize());
        scene.add("show_independent_immediate", data);
    }

    @ZenMethod
    public void makeSectionIndependent(String handle, ScriptSelection selection) {
        scene.defineHandle(handle);
        NBTTagCompound data = new NBTTagCompound();
        data.setString("handle", handle);
        data.setTag("selection", required(selection).serialize());
        scene.add("make_independent", data);
    }

    @ZenMethod
    public void showSectionAndMerge(ScriptSelection selection, String direction, String handle) {
        scene.requireHandle(handle, ScriptSceneBuilder.HandleType.SECTION);
        NBTTagCompound data = selection(selection, direction);
        data.setString("handle", handle);
        scene.add("show_section_merge", data);
    }

    @ZenMethod
    public void glueBlockOnto(int x, int y, int z, String direction, String handle) {
        scene.requireHandle(handle, ScriptSceneBuilder.HandleType.SECTION);
        NBTTagCompound data = position(x, y, z);
        data.setString("direction", direction(direction));
        data.setString("handle", handle);
        scene.add("glue_block", data);
    }

    @ZenMethod
    public void glueBlockOnto(ScriptPosition position, String direction, String handle) {
        ScriptPosition required = ScriptPosition.require(position, "Block position");
        glueBlockOnto(required.x, required.y, required.z, direction, handle);
    }

    @ZenMethod
    public void hideIndependentSection(String handle, String direction) {
        scene.requireHandle(handle, ScriptSceneBuilder.HandleType.SECTION);
        NBTTagCompound data = new NBTTagCompound();
        data.setString("handle", handle);
        data.setString("direction", direction(direction));
        scene.add("hide_independent", data);
    }

    @ZenMethod
    public void moveSection(String handle, double x, double y, double z, int duration) {
        scene.requireHandle(handle, ScriptSceneBuilder.HandleType.SECTION);
        NBTTagCompound data = vector(x, y, z);
        data.setString("handle", handle);
        data.setInteger("duration", duration(duration));
        scene.add("move_section", data);
    }

    @ZenMethod
    public void moveSection(String handle, ScriptVector offset, int duration) {
        ScriptVector required = ScriptVector.require(offset, "Section movement");
        moveSection(handle, required.x, required.y, required.z, duration);
    }

    @ZenMethod
    public void rotateSection(String handle, double x, double y, double z, int duration) {
        scene.requireHandle(handle, ScriptSceneBuilder.HandleType.SECTION);
        NBTTagCompound data = vector(x, y, z);
        data.setString("handle", handle);
        data.setInteger("duration", duration(duration));
        scene.add("rotate_section", data);
    }

    @ZenMethod
    public void rotateSection(String handle, ScriptVector rotation, int duration) {
        ScriptVector required = ScriptVector.require(rotation, "Section rotation");
        rotateSection(handle, required.x, required.y, required.z, duration);
    }

    @ZenMethod
    public void configureCenterOfRotation(String handle, double x, double y, double z) {
        scene.requireHandle(handle, ScriptSceneBuilder.HandleType.SECTION);
        NBTTagCompound data = vector(x, y, z);
        data.setString("handle", handle);
        scene.add("center_section", data);
    }

    @ZenMethod
    public void configureCenterOfRotation(String handle, ScriptVector anchor) {
        ScriptVector required = ScriptVector.require(anchor, "Center of rotation");
        configureCenterOfRotation(handle, required.x, required.y, required.z);
    }

    @ZenMethod
    public void configureStabilization(String handle, double x, double y, double z) {
        scene.requireHandle(handle, ScriptSceneBuilder.HandleType.SECTION);
        NBTTagCompound data = vector(x, y, z);
        data.setString("handle", handle);
        scene.add("stabilize_section", data);
    }

    @ZenMethod
    public void configureStabilization(String handle, ScriptVector anchor) {
        ScriptVector required = ScriptVector.require(anchor, "Stabilization anchor");
        configureStabilization(handle, required.x, required.y, required.z);
    }

    @ZenMethod
    public void setBlock(int x, int y, int z, String state, boolean particles) {
        ScriptBlockStateParser.parse(requiredText(state, "block state"));
        NBTTagCompound data = position(x, y, z);
        data.setString("state", requiredText(state, "block state"));
        data.setBoolean("particles", particles);
        scene.add("set_block", data);
    }

    @ZenMethod
    public void setBlock(ScriptPosition position, String state, boolean particles) {
        ScriptPosition required = ScriptPosition.require(position, "Block position");
        setBlock(required.x, required.y, required.z, state, particles);
    }

    @ZenMethod
    public void setBlocks(ScriptSelection selection, String state, boolean particles) {
        ScriptBlockStateParser.parse(requiredText(state, "block state"));
        NBTTagCompound data = new NBTTagCompound();
        data.setTag("selection", required(selection).serialize());
        data.setString("state", requiredText(state, "block state"));
        data.setBoolean("particles", particles);
        scene.add("set_blocks", data);
    }

    @ZenMethod
    public void replaceBlocks(ScriptSelection selection, String state, boolean particles) {
        setBlocks(selection, state, particles);
    }

    @ZenMethod
    public void destroyBlock(int x, int y, int z) {
        scene.add("destroy_block", position(x, y, z));
    }

    @ZenMethod
    public void destroyBlock(ScriptPosition position) {
        ScriptPosition required = ScriptPosition.require(position, "Block position");
        destroyBlock(required.x, required.y, required.z);
    }

    @ZenMethod
    public void incrementBlockBreakingProgress(int x, int y, int z) {
        scene.add("break_progress", position(x, y, z));
    }

    @ZenMethod
    public void incrementBlockBreakingProgress(ScriptPosition position) {
        ScriptPosition required = ScriptPosition.require(position, "Block position");
        incrementBlockBreakingProgress(required.x, required.y, required.z);
    }

    @ZenMethod
    public void cycleBlockProperty(int x, int y, int z, String property) {
        NBTTagCompound data = position(x, y, z);
        data.setString("property", requiredText(property, "block property"));
        scene.add("cycle_property", data);
    }

    @ZenMethod
    public void cycleBlockProperty(ScriptPosition position, String property) {
        ScriptPosition required = ScriptPosition.require(position, "Block position");
        cycleBlockProperty(required.x, required.y, required.z, property);
    }

    @ZenMethod
    public void toggleRedstonePower(ScriptSelection selection) {
        NBTTagCompound data = new NBTTagCompound();
        data.setTag("selection", required(selection).serialize());
        scene.add("toggle_redstone", data);
    }

    @ZenMethod
    public void createItemEntity(String handle, double x, double y, double z, double motionX, double motionY,
                                 double motionZ, String itemId, int count, int meta) {
        scene.defineHandle(handle, ScriptSceneBuilder.HandleType.ITEM);
        if (count < 1 || count > 64) throw new IllegalArgumentException("Item count must be 1..64");
        NBTTagCompound data = vector(x, y, z);
        data.setString("handle", handle);
        data.setDouble("mx", motionX); data.setDouble("my", motionY); data.setDouble("mz", motionZ);
        data.setString("item", ScriptSceneRegistry.parseId(itemId, "item id").toString());
        data.setInteger("count", count); data.setInteger("meta", meta);
        scene.add("create_item", data);
    }

    @ZenMethod
    public void createItemEntity(String handle, ScriptVector position, ScriptVector motion,
                                 String itemId, int count, int meta) {
        ScriptVector requiredPosition = ScriptVector.require(position, "Item position");
        ScriptVector requiredMotion = ScriptVector.require(motion, "Item motion");
        createItemEntity(handle, requiredPosition.x, requiredPosition.y, requiredPosition.z,
            requiredMotion.x, requiredMotion.y, requiredMotion.z, itemId, count, meta);
    }

    @ZenMethod
    public void moveItem(String handle, double x, double y, double z, int duration) {
        scene.requireHandle(handle, ScriptSceneBuilder.HandleType.ITEM);
        NBTTagCompound data = vector(x, y, z);
        data.setString("handle", handle);
        data.setInteger("duration", duration(duration));
        scene.add("move_item", data);
    }

    @ZenMethod
    public void moveItem(String handle, ScriptVector offset, int duration) {
        ScriptVector required = ScriptVector.require(offset, "Item movement");
        moveItem(handle, required.x, required.y, required.z, duration);
    }

    @ZenMethod
    public void setItemVisible(String handle, boolean visible) {
        scene.requireHandle(handle, ScriptSceneBuilder.HandleType.ITEM);
        NBTTagCompound data = new NBTTagCompound();
        data.setString("handle", handle);
        data.setBoolean("visible", visible);
        scene.add("set_item_visible", data);
    }

    @ZenMethod
    public void hideItem(String handle) {
        setItemVisible(handle, false);
    }

    @ZenMethod
    public void showItem(String handle) {
        setItemVisible(handle, true);
    }

    @ZenMethod
    public void removeItem(String handle) {
        scene.requireHandle(handle, ScriptSceneBuilder.HandleType.ITEM);
        NBTTagCompound data = new NBTTagCompound();
        data.setString("handle", handle);
        scene.add("remove_item", data);
        scene.terminateHandle(handle, ScriptSceneBuilder.HandleType.ITEM);
    }

    @ZenMethod
    public void createMinecart(String handle, double x, double y, double z, float angle, String type) {
        scene.defineHandle(handle, ScriptSceneBuilder.HandleType.MINECART);
        NBTTagCompound data = vector(x, y, z);
        data.setString("handle", handle);
        data.setFloat("angle", angle);
        data.setString("type", minecartType(type));
        scene.add("create_minecart", data);
    }

    @ZenMethod
    public void createMinecart(String handle, ScriptVector position, float angle, String type) {
        ScriptVector required = ScriptVector.require(position, "Minecart position");
        createMinecart(handle, required.x, required.y, required.z, angle, type);
    }

    @ZenMethod
    public void createCart(String handle, double x, double y, double z, float angle, String type) {
        createMinecart(handle, x, y, z, angle, type);
    }

    @ZenMethod
    public void createCart(String handle, ScriptVector position, float angle, String type) {
        createMinecart(handle, position, angle, type);
    }

    @ZenMethod
    public void moveMinecart(String handle, double x, double y, double z, int duration) {
        scene.requireHandle(handle, ScriptSceneBuilder.HandleType.MINECART);
        NBTTagCompound data = vector(x, y, z);
        data.setString("handle", handle);
        data.setInteger("duration", duration(duration));
        scene.add("move_minecart", data);
    }

    @ZenMethod
    public void moveMinecart(String handle, ScriptVector offset, int duration) {
        ScriptVector required = ScriptVector.require(offset, "Minecart movement");
        moveMinecart(handle, required.x, required.y, required.z, duration);
    }

    @ZenMethod
    public void moveCart(String handle, double x, double y, double z, int duration) {
        moveMinecart(handle, x, y, z, duration);
    }

    @ZenMethod
    public void moveCart(String handle, ScriptVector offset, int duration) {
        moveMinecart(handle, offset, duration);
    }

    @ZenMethod
    public void rotateMinecart(String handle, float angle, int duration) {
        scene.requireHandle(handle, ScriptSceneBuilder.HandleType.MINECART);
        NBTTagCompound data = new NBTTagCompound();
        data.setString("handle", handle);
        data.setFloat("angle", angle);
        data.setInteger("duration", duration(duration));
        scene.add("rotate_minecart", data);
    }

    @ZenMethod
    public void rotateCart(String handle, float angle, int duration) {
        rotateMinecart(handle, angle, duration);
    }

    @ZenMethod
    public void hideMinecart(String handle, String direction) {
        scene.requireHandle(handle, ScriptSceneBuilder.HandleType.MINECART);
        NBTTagCompound data = new NBTTagCompound();
        data.setString("handle", handle);
        data.setString("direction", direction(direction));
        scene.add("hide_minecart", data);
    }

    @ZenMethod
    public void createParrot(String handle, double x, double y, double z, String pose) {
        scene.defineHandle(handle, ScriptSceneBuilder.HandleType.PARROT);
        NBTTagCompound data = vector(x, y, z);
        data.setString("handle", handle);
        data.setString("pose", parrotPose(pose));
        scene.add("create_parrot", data);
    }

    @ZenMethod
    public void createParrot(String handle, ScriptVector position, String pose) {
        ScriptVector required = ScriptVector.require(position, "Parrot position");
        createParrot(handle, required.x, required.y, required.z, pose);
    }

    @ZenMethod
    public void createBirb(String handle, double x, double y, double z, String pose) {
        createParrot(handle, x, y, z, pose);
    }

    @ZenMethod
    public void createBirb(String handle, ScriptVector position, String pose) {
        createParrot(handle, position, pose);
    }

    @ZenMethod
    public void changeParrotPose(String handle, String pose) {
        scene.requireHandle(handle, ScriptSceneBuilder.HandleType.PARROT);
        NBTTagCompound data = new NBTTagCompound();
        data.setString("handle", handle);
        data.setString("pose", parrotPose(pose));
        scene.add("change_parrot_pose", data);
    }

    @ZenMethod
    public void changeBirbPose(String handle, String pose) {
        changeParrotPose(handle, pose);
    }

    @ZenMethod
    public void moveParrot(String handle, double x, double y, double z, int duration) {
        scene.requireHandle(handle, ScriptSceneBuilder.HandleType.PARROT);
        NBTTagCompound data = vector(x, y, z);
        data.setString("handle", handle);
        data.setInteger("duration", duration(duration));
        scene.add("move_parrot", data);
    }

    @ZenMethod
    public void moveParrot(String handle, ScriptVector offset, int duration) {
        ScriptVector required = ScriptVector.require(offset, "Parrot movement");
        moveParrot(handle, required.x, required.y, required.z, duration);
    }

    @ZenMethod
    public void rotateParrot(String handle, double x, double y, double z, int duration) {
        scene.requireHandle(handle, ScriptSceneBuilder.HandleType.PARROT);
        NBTTagCompound data = vector(x, y, z);
        data.setString("handle", handle);
        data.setInteger("duration", duration(duration));
        scene.add("rotate_parrot", data);
    }

    @ZenMethod
    public void rotateParrot(String handle, ScriptVector rotation, int duration) {
        ScriptVector required = ScriptVector.require(rotation, "Parrot rotation");
        rotateParrot(handle, required.x, required.y, required.z, duration);
    }

    @ZenMethod
    public void hideParrot(String handle, String direction) {
        scene.requireHandle(handle, ScriptSceneBuilder.HandleType.PARROT);
        NBTTagCompound data = new NBTTagCompound();
        data.setString("handle", handle);
        data.setString("direction", direction(direction));
        scene.add("hide_parrot", data);
    }

    @ZenMethod
    public void modifyTileNBT(ScriptSelection selection, IData value, boolean replace, boolean redraw) {
        if (value == null) throw new IllegalArgumentException("Tile NBT data is required");
        NBTBase converted = NBTConverter.from(value);
        if (!(converted instanceof NBTTagCompound))
            throw new IllegalArgumentException("Tile NBT must be a data map");
        NBTTagCompound nbt = (NBTTagCompound) converted;
        if (nbt.toString().length() > 262144)
            throw new IllegalArgumentException("Tile NBT exceeds 256 KiB text safety limit");
        NBTTagCompound data = new NBTTagCompound();
        data.setTag("selection", required(selection).serialize());
        data.setTag("nbt", nbt.copy());
        data.setBoolean("replace", replace);
        data.setBoolean("redraw", redraw);
        scene.add("tile_nbt", data);
    }

    private static NBTTagCompound selection(ScriptSelection selection, String direction) {
        NBTTagCompound data = new NBTTagCompound();
        data.setTag("selection", required(selection).serialize());
        data.setString("direction", direction(direction));
        return data;
    }

    private static ScriptSelection required(ScriptSelection selection) {
        if (selection == null) throw new IllegalArgumentException("Selection is required");
        return selection;
    }

    static String direction(String value) {
        String normalized = requiredText(value, "direction").toLowerCase(java.util.Locale.ROOT);
        if (!normalized.equals("up") && !normalized.equals("down") && !normalized.equals("north")
            && !normalized.equals("south") && !normalized.equals("west") && !normalized.equals("east"))
            throw new IllegalArgumentException("Unknown direction: " + value);
        return normalized;
    }

    static String requiredText(String value, String label) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(label + " is required");
        if (value.length() > 8192) throw new IllegalArgumentException(label + " exceeds 8192 characters");
        return value;
    }

    static NBTTagCompound position(int x, int y, int z) {
        NBTTagCompound data = new NBTTagCompound();
        data.setInteger("x", x); data.setInteger("y", y); data.setInteger("z", z);
        return data;
    }

    static NBTTagCompound vector(double x, double y, double z) {
        ScriptVector.validate(x, y, z, "Vector");
        NBTTagCompound data = new NBTTagCompound();
        data.setDouble("x", x); data.setDouble("y", y); data.setDouble("z", z);
        return data;
    }

    static String minecartType(String value) {
        String normalized = requiredText(value, "minecart type").toLowerCase(java.util.Locale.ROOT);
        if (!normalized.equals("empty") && !normalized.equals("chest") && !normalized.equals("furnace")
            && !normalized.equals("hopper") && !normalized.equals("tnt"))
            throw new IllegalArgumentException("Unknown minecart type: " + value);
        return normalized;
    }

    static String parrotPose(String value) {
        String normalized = requiredText(value, "parrot pose").toLowerCase(java.util.Locale.ROOT);
        if (normalized.equals("face_point_of_interest")) normalized = "face_poi";
        if (!normalized.equals("dance") && !normalized.equals("flappy")
            && !normalized.equals("face_poi") && !normalized.equals("face_cursor"))
            throw new IllegalArgumentException("Unknown parrot pose: " + value);
        return normalized;
    }

    private static int duration(int duration) {
        if (duration < 0 || duration > 72000) throw new IllegalArgumentException("Duration must be 0..72000");
        return duration;
    }
}
