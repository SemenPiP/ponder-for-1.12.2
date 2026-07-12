package net.createmod.ponder.mixin;

import java.util.ArrayDeque;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.world.World;

@Mixin(ParticleManager.class)
public interface ParticleManagerAccessor {
    @Accessor("particleTypes")
    Map<Integer, IParticleFactory> ponder$getParticleFactories();

    @Accessor("fxLayers")
    ArrayDeque<Particle>[][] ponder$getParticleLayers();

    @Accessor("world")
    World ponder$getParticleWorld();
}
