package net.createmod.ponder.script;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.data.IData;
import crafttweaker.mc1120.data.NBTConverter;
import net.createmod.ponder.api.script.ScriptInstructionCodec;
import net.createmod.ponder.api.script.ScriptInstructionCodecs;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
import stanhebben.zenscript.annotations.ZenProperty;

@ZenRegister
@ZenClass("mods.ponder.SceneBuilder")
public final class ScriptSceneBuilder {
    private final ResourceLocation component;
    private final ResourceLocation sceneId;
    private final String title;
    private final ResourceLocation structure;
    private final List<ScriptInstruction> instructions = new ArrayList<ScriptInstruction>();
    private final Set<ResourceLocation> tags = new LinkedHashSet<ResourceLocation>();
    private final java.util.Map<String, HandleType> handles = new java.util.LinkedHashMap<String, HandleType>();
    @ZenProperty("world")
    public final ScriptWorldBuilder world = new ScriptWorldBuilder(this);
    @ZenProperty("overlay")
    public final ScriptOverlayBuilder overlay = new ScriptOverlayBuilder(this);
    @ZenProperty("effects")
    public final ScriptEffectsBuilder effects = new ScriptEffectsBuilder(this);
    private boolean clientOnly;
    private boolean registered;

    ScriptSceneBuilder(String componentId, String sceneId, String title, String structureId) {
        this.component = ScriptSceneRegistry.parseId(componentId, "component id");
        this.sceneId = ScriptSceneRegistry.parseId(sceneId, "scene id");
        this.title = title;
        this.structure = ScriptSceneRegistry.parseId(structureId, "structure id");
    }

    public ScriptWorldBuilder getWorld() { return world; }

    public ScriptOverlayBuilder getOverlay() { return overlay; }

    public ScriptEffectsBuilder getEffects() { return effects; }

    @ZenMethod
    public ScriptSceneBuilder tag(String tagId) {
        tags.add(ScriptSceneRegistry.parseId(tagId, "tag id"));
        return this;
    }

    @ZenMethod
    public ScriptSceneBuilder clientOnly() {
        clientOnly = true;
        return this;
    }

    @ZenMethod
    public ScriptSceneBuilder configureBasePlate(int x, int z, int size) {
        if (size < 1 || size > 256) throw new IllegalArgumentException("Base plate size must be 1..256");
        NBTTagCompound data = new NBTTagCompound();
        data.setInteger("x", x); data.setInteger("z", z); data.setInteger("size", size);
        return add("configure_base_plate", data);
    }

    @ZenMethod public ScriptSceneBuilder showBasePlate() { return add("show_base_plate"); }
    @ZenMethod public ScriptSceneBuilder removeShadow() { return add("remove_shadow"); }

    @ZenMethod
    public ScriptSceneBuilder scaleSceneView(float scale) {
        if (!(scale > 0) || scale > 16) throw new IllegalArgumentException("Scene scale must be > 0 and <= 16");
        NBTTagCompound data = new NBTTagCompound(); data.setFloat("value", scale);
        return add("scale", data);
    }

    @ZenMethod
    public ScriptSceneBuilder setSceneOffsetY(float offset) {
        NBTTagCompound data = new NBTTagCompound(); data.setFloat("value", offset);
        return add("offset_y", data);
    }

    @ZenMethod
    public ScriptSceneBuilder idle(int ticks) {
        if (ticks < 0 || ticks > 72000) throw new IllegalArgumentException("Idle ticks must be 0..72000");
        NBTTagCompound data = new NBTTagCompound(); data.setInteger("ticks", ticks);
        return add("idle", data);
    }

    @ZenMethod
    public ScriptSceneBuilder idleSeconds(int seconds) {
        return idle(Math.multiplyExact(seconds, 20));
    }

    @ZenMethod
    public ScriptSceneBuilder rotateCameraY(float degrees) {
        NBTTagCompound data = new NBTTagCompound(); data.setFloat("degrees", degrees);
        return add("rotate_camera", data);
    }

    @ZenMethod public ScriptSceneBuilder addKeyframe() { return add("keyframe"); }
    @ZenMethod public ScriptSceneBuilder addLazyKeyframe() { return add("lazy_keyframe"); }
    @ZenMethod public ScriptSceneBuilder markAsFinished() { return add("finish"); }

    @ZenMethod
    public ScriptSceneBuilder setNextUpEnabled(boolean enabled) {
        NBTTagCompound data = new NBTTagCompound(); data.setBoolean("enabled", enabled);
        return add("next_up", data);
    }

    @ZenMethod
    public ScriptSceneBuilder movePointOfInterest(double x, double y, double z) {
        return add("move_poi", ScriptWorldBuilder.vector(x, y, z));
    }

    @ZenMethod
    public ScriptSceneBuilder custom(String codecId, IData value) {
        ResourceLocation id = ScriptSceneRegistry.parseId(codecId, "instruction codec id");
        ScriptInstructionCodec codec = ScriptInstructionCodecs.get(id);
        if (codec == null) throw new IllegalArgumentException("Unknown Ponder script instruction codec " + id);
        NBTBase converted = value == null ? new NBTTagCompound() : NBTConverter.from(value);
        if (!(converted instanceof NBTTagCompound))
            throw new IllegalArgumentException("Custom instruction data must be a data map");
        NBTTagCompound payload = (NBTTagCompound) converted;
        codec.validate(payload.copy());
        NBTTagCompound data = new NBTTagCompound();
        data.setString("codec", id.toString());
        data.setTag("payload", payload.copy());
        return add("custom", data);
    }

    @ZenMethod
    public void register() {
        ensureMutable();
        ScriptSceneRegistry.registrationAttempted(this);
        ScriptSceneRegistry.register(new ScriptSceneDefinition(component, sceneId, title, structure,
            new ArrayList<ResourceLocation>(tags), instructions, clientOnly));
        registered = true;
    }

    ScriptSceneBuilder add(String operation) {
        return add(operation, new NBTTagCompound());
    }

    ScriptSceneBuilder add(String operation, NBTTagCompound data) {
        ensureMutable();
        if (instructions.size() >= ScriptSceneDefinition.MAX_INSTRUCTIONS)
            throw new IllegalStateException("Scene exceeds instruction limit: " + sceneId);
        instructions.add(new ScriptInstruction(operation, data));
        return this;
    }

    void defineHandle(String handle) {
        defineHandle(handle, HandleType.SECTION);
    }

    void defineHandle(String handle, HandleType type) {
        validateHandle(handle);
        if (type == null) throw new IllegalArgumentException("Handle type is required");
        if (handles.put(handle, type) != null) throw new IllegalArgumentException("Duplicate scene handle: " + handle);
    }

    void requireHandle(String handle) {
        requireHandle(handle, null);
    }

    void requireHandle(String handle, HandleType expected) {
        validateHandle(handle);
        HandleType actual = handles.get(handle);
        if (actual == null) throw new IllegalArgumentException("Unknown scene handle: " + handle);
        if (expected != null && actual != expected)
            throw new IllegalArgumentException("Scene handle '" + handle + "' is " + actual.scriptName
                + ", expected " + expected.scriptName);
    }

    static void validateHandle(String handle) {
        if (handle == null || !handle.matches("[A-Za-z0-9_.-]{1,64}"))
            throw new IllegalArgumentException("Handle must match [A-Za-z0-9_.-]{1,64}");
    }

    enum HandleType {
        SECTION("section"),
        ITEM("item"),
        MINECART("minecart"),
        PARROT("parrot");

        final String scriptName;

        HandleType(String scriptName) {
            this.scriptName = scriptName;
        }
    }

    ResourceLocation getSceneId() {
        return sceneId;
    }

    private void ensureMutable() {
        if (registered) throw new IllegalStateException("Scene is already registered: " + sceneId);
    }
}
