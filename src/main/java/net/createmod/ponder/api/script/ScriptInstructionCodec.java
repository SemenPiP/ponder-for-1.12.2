package net.createmod.ponder.api.script;

import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

/** Extension point for deterministic, serializable ZenScript scene instructions. */
public interface ScriptInstructionCodec {
    ResourceLocation getId();

    void validate(NBTTagCompound data);

    void program(NBTTagCompound data, SceneBuilder scene, SceneBuildingUtil util);
}
