package net.createmod.ponder.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.createmod.ponder.api.scene.PonderStoryBoard;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;

public final class ScriptSceneDefinition {
    public static final int FORMAT_VERSION = 1;
    public static final int MAX_INSTRUCTIONS = 4096;

    private final ResourceLocation component;
    private final ResourceLocation sceneId;
    private final String title;
    private final ResourceLocation structure;
    private final List<ResourceLocation> tags;
    private final List<ScriptInstruction> instructions;
    private final boolean clientOnly;

    public ScriptSceneDefinition(ResourceLocation component, ResourceLocation sceneId, String title,
                                 ResourceLocation structure, List<ResourceLocation> tags,
                                 List<ScriptInstruction> instructions, boolean clientOnly) {
        if (component == null || sceneId == null || structure == null)
            throw new IllegalArgumentException("Component, scene id and structure are required");
        validateId(component, "component");
        validateId(sceneId, "scene");
        validateId(structure, "structure");
        if (title == null || title.trim().isEmpty())
            throw new IllegalArgumentException("Scene title is required for " + sceneId);
        if (title.length() > 8192)
            throw new IllegalArgumentException("Scene title exceeds 8192 characters: " + sceneId);
        if (instructions == null || instructions.isEmpty())
            throw new IllegalArgumentException("Scene has no instructions: " + sceneId);
        if (instructions.size() > MAX_INSTRUCTIONS)
            throw new IllegalArgumentException("Scene exceeds " + MAX_INSTRUCTIONS + " instructions: " + sceneId);
        ScriptInstructionValidator.validate(sceneId, instructions);
        this.component = component;
        this.sceneId = sceneId;
        this.title = title;
        this.structure = structure;
        if (tags == null) throw new IllegalArgumentException("Scene tags are required: " + sceneId);
        for (ResourceLocation tag : tags) validateId(tag, "tag");
        this.tags = Collections.unmodifiableList(new ArrayList<ResourceLocation>(tags));
        this.instructions = Collections.unmodifiableList(new ArrayList<ScriptInstruction>(instructions));
        this.clientOnly = clientOnly;
    }

    public ResourceLocation getComponent() { return component; }
    public ResourceLocation getSceneId() { return sceneId; }
    public String getTitle() { return title; }
    public ResourceLocation getStructure() { return structure; }
    public List<ResourceLocation> getTags() { return tags; }
    public List<ScriptInstruction> getInstructions() { return instructions; }
    public boolean isClientOnly() { return clientOnly; }

    public PonderStoryBoard asStoryBoard() {
        return (scene, util) -> ScriptSceneProgram.program(this, scene, util);
    }

    public NBTTagCompound serialize() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("format", FORMAT_VERSION);
        tag.setString("component", component.toString());
        tag.setString("scene", sceneId.toString());
        tag.setString("title", title);
        tag.setString("structure", structure.toString());
        tag.setBoolean("clientOnly", clientOnly);
        NBTTagList tagList = new NBTTagList();
        for (ResourceLocation value : tags) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setString("id", value.toString());
            tagList.appendTag(entry);
        }
        tag.setTag("tags", tagList);
        NBTTagList instructionList = new NBTTagList();
        for (ScriptInstruction instruction : instructions)
            instructionList.appendTag(instruction.serialize());
        tag.setTag("instructions", instructionList);
        return tag;
    }

    public static ScriptSceneDefinition deserialize(NBTTagCompound tag) {
        if (tag.getInteger("format") != FORMAT_VERSION)
            throw new IllegalArgumentException("Unsupported Ponder script format " + tag.getInteger("format"));
        List<ResourceLocation> tags = new ArrayList<ResourceLocation>();
        NBTTagList tagList = tag.getTagList("tags", 10);
        for (int i = 0; i < tagList.tagCount(); i++)
            tags.add(new ResourceLocation(tagList.getCompoundTagAt(i).getString("id")));
        List<ScriptInstruction> instructions = new ArrayList<ScriptInstruction>();
        NBTTagList instructionList = tag.getTagList("instructions", 10);
        for (int i = 0; i < instructionList.tagCount(); i++)
            instructions.add(ScriptInstruction.deserialize(instructionList.getCompoundTagAt(i)));
        return new ScriptSceneDefinition(
            new ResourceLocation(tag.getString("component")),
            new ResourceLocation(tag.getString("scene")),
            tag.getString("title"),
            new ResourceLocation(tag.getString("structure")),
            tags, instructions, tag.getBoolean("clientOnly"));
    }

    private static void validateId(ResourceLocation id, String label) {
        if (id == null || id.toString().length() > 256)
            throw new IllegalArgumentException("Scene " + label + " id exceeds 256 characters");
    }
}
