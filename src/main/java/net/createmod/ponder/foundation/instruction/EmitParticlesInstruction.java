package net.createmod.ponder.foundation.instruction;

import net.createmod.ponder.api.ParticleEmitter;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.util.math.Vec3d;

public final class EmitParticlesInstruction extends TickingInstruction {
    private final Vec3d location;
    private final ParticleEmitter emitter;
    private final float amount;
    private float accumulator;

    public EmitParticlesInstruction(Vec3d location, ParticleEmitter emitter, float amountPerCycle, int cycles) {
        super(false, Math.max(0, cycles));
        if (location == null || emitter == null || amountPerCycle < 0)
            throw new IllegalArgumentException("Invalid particle instruction");
        this.location = location; this.emitter = emitter; this.amount = amountPerCycle;
    }

    @Override public void reset(PonderScene scene) { super.reset(scene); accumulator = 0; }
    @Override protected void tickRunning(PonderScene scene, int elapsed, float progress) {
        if (scene.getWorld() == null) return;
        accumulator += amount;
        int count = (int) accumulator;
        accumulator -= count;
        for (int i = 0; i < count; i++) emitter.create(scene.getWorld(), location.x, location.y, location.z);
    }
}
