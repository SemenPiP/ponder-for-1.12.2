package net.createmod.ponder.api.scene;

import net.createmod.ponder.api.ParticleEmitter;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public interface EffectInstructions {
    void emitParticles(Vec3d location, ParticleEmitter emitter, float amountPerCycle, int cycles);
    ParticleEmitter simpleParticleEmitter(EnumParticleTypes type, Vec3d motion, int... arguments);
    ParticleEmitter particleEmitterWithinBlockSpace(EnumParticleTypes type, Vec3d motion, int... arguments);
    void indicateRedstone(BlockPos pos);
    void indicateSuccess(BlockPos pos);
    void createRedstoneParticles(BlockPos pos, int color, int amount);
}
