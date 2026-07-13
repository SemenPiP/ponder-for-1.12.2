package net.createmod.ponder.script;

import crafttweaker.annotations.ZenRegister;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.ponder.Effects")
public final class ScriptEffectsBuilder {
    private final ScriptSceneBuilder scene;

    ScriptEffectsBuilder(ScriptSceneBuilder scene) {
        this.scene = scene;
    }

    @ZenMethod
    public void indicateRedstone(int x, int y, int z) {
        scene.add("indicate_redstone", ScriptWorldBuilder.position(x, y, z));
    }

    @ZenMethod
    public void indicateSuccess(int x, int y, int z) {
        scene.add("indicate_success", ScriptWorldBuilder.position(x, y, z));
    }

    @ZenMethod
    public void createRedstoneParticles(int x, int y, int z, int color, int amount) {
        if (amount < 0 || amount > 4096) throw new IllegalArgumentException("Particle amount must be 0..4096");
        NBTTagCompound data = ScriptWorldBuilder.position(x, y, z);
        data.setInteger("color", color);
        data.setInteger("amount", amount);
        scene.add("redstone_particles", data);
    }

    @ZenMethod
    public void emitParticles(String type, double x, double y, double z, double motionX, double motionY,
                              double motionZ, float amount, int cycles) {
        NBTTagCompound data = ScriptWorldBuilder.vector(x, y, z);
        String normalized = ScriptWorldBuilder.requiredText(type, "particle type")
            .toLowerCase(java.util.Locale.ROOT);
        EnumParticleTypes.valueOf(normalized.toUpperCase(java.util.Locale.ROOT));
        if (!Float.isFinite(amount) || amount < 0 || amount > 4096)
            throw new IllegalArgumentException("Particle amount must be 0..4096");
        if (cycles < 0 || cycles > 72000)
            throw new IllegalArgumentException("Particle cycles must be 0..72000");
        data.setString("type", normalized);
        data.setDouble("mx", motionX); data.setDouble("my", motionY); data.setDouble("mz", motionZ);
        data.setFloat("amount", amount); data.setInteger("cycles", cycles);
        scene.add("particles", data);
    }

    @ZenMethod
    public void emitParticlesWithinBlock(String type, double x, double y, double z, double motionX, double motionY,
                                         double motionZ, float amount, int cycles) {
        NBTTagCompound data = particleData(type, x, y, z, motionX, motionY, motionZ, amount, cycles);
        scene.add("particles_within_block", data);
    }

    @ZenMethod
    public void movePointOfInterest(double x, double y, double z) {
        scene.add("move_poi", ScriptWorldBuilder.vector(x, y, z));
    }

    private static NBTTagCompound particleData(String type, double x, double y, double z, double motionX,
                                               double motionY, double motionZ, float amount, int cycles) {
        NBTTagCompound data = ScriptWorldBuilder.vector(x, y, z);
        String normalized = ScriptWorldBuilder.requiredText(type, "particle type")
            .toLowerCase(java.util.Locale.ROOT);
        EnumParticleTypes.valueOf(normalized.toUpperCase(java.util.Locale.ROOT));
        if (!Float.isFinite(amount) || amount < 0 || amount > 4096)
            throw new IllegalArgumentException("Particle amount must be 0..4096");
        if (cycles < 0 || cycles > 72000)
            throw new IllegalArgumentException("Particle cycles must be 0..72000");
        data.setString("type", normalized);
        data.setDouble("mx", motionX); data.setDouble("my", motionY); data.setDouble("mz", motionZ);
        data.setFloat("amount", amount); data.setInteger("cycles", cycles);
        return data;
    }
}
