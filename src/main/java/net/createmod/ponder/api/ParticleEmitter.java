package net.createmod.ponder.api;

import net.createmod.ponder.foundation.PonderWorld;

@FunctionalInterface
public interface ParticleEmitter {
    void create(PonderWorld world, double x, double y, double z);
}
