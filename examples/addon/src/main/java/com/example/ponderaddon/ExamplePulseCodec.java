package com.example.ponderaddon;

import java.util.Collections;
import java.util.Set;

import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.script.ScriptInstructionCodec;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

/** Deterministic custom instruction used by the installable addon smoke pack. */
public final class ExamplePulseCodec implements ScriptInstructionCodec {
    public static final ResourceLocation ID =
        new ResourceLocation(ExampleAddon.MOD_ID, "pulse");
    public static final ResourceLocation OUTLINE_CAPABILITY =
        new ResourceLocation(ExampleAddon.MOD_ID, "outline");

    @Override public ResourceLocation getId() { return ID; }
    @Override public int getProtocolVersion() { return 1; }

    @Override
    public Set<ResourceLocation> getCapabilities() {
        return Collections.singleton(OUTLINE_CAPABILITY);
    }

    @Override
    public Set<ResourceLocation> getRequiredCapabilities(NBTTagCompound data) {
        return Collections.singleton(OUTLINE_CAPABILITY);
    }

    @Override
    public void validate(NBTTagCompound data) {
        if (data == null)
            throw new IllegalArgumentException("Pulse payload is required");
        int duration = data.getInteger("duration");
        if (duration < 1 || duration > 200)
            throw new IllegalArgumentException("Pulse duration must be 1..200");
        String color = data.getString("color");
        try {
            PonderPalette.valueOf(color.toUpperCase(java.util.Locale.ROOT));
        } catch (RuntimeException invalid) {
            throw new IllegalArgumentException("Unknown Ponder pulse color: " + color, invalid);
        }
        requireCoordinate(data, "x");
        requireCoordinate(data, "y");
        requireCoordinate(data, "z");
    }

    @Override
    public void program(NBTTagCompound data, SceneBuilder scene, SceneBuildingUtil util) {
        validate(data);
        BlockPos position = new BlockPos(data.getInteger("x"), data.getInteger("y"), data.getInteger("z"));
        PonderPalette color = PonderPalette.valueOf(
            data.getString("color").toUpperCase(java.util.Locale.ROOT));
        int duration = data.getInteger("duration");
        scene.overlay().showOutline(color, ID.toString(), util.select().position(position), duration);
        scene.effects().indicateSuccess(position);
        scene.idle(duration);
    }

    private static void requireCoordinate(NBTTagCompound data, String key) {
        if (!data.hasKey(key, 99))
            throw new IllegalArgumentException("Pulse coordinate " + key + " is required");
        int value = data.getInteger(key);
        if (value < -4096 || value > 4096)
            throw new IllegalArgumentException("Pulse coordinate " + key + " is outside -4096..4096");
    }
}
