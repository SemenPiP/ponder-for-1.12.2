package net.createmod.ponder.api.script;

import java.util.Collections;
import java.util.Set;

import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

/** Extension point for deterministic, serializable ZenScript scene instructions. */
public interface ScriptInstructionCodec {
    ResourceLocation getId();

    default int getProtocolVersion() {
        return 1;
    }

    default Set<ResourceLocation> getCapabilities() {
        return Collections.emptySet();
    }

    default Set<ResourceLocation> getRequiredCapabilities(NBTTagCompound data) {
        return Collections.emptySet();
    }

    void validate(NBTTagCompound data);

    void program(NBTTagCompound data, SceneBuilder scene, SceneBuildingUtil util);
}
