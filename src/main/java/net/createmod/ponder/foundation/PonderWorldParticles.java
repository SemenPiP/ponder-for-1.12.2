package net.createmod.ponder.foundation;

import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Particle manager isolated to a virtual PonderLevel. */
@SideOnly(Side.CLIENT)
public final class PonderWorldParticles {
    private final ParticleManager manager;
    public PonderWorldParticles(PonderLevel world) {
        if (world == null) throw new IllegalArgumentException("Ponder level is required");
        manager = new ParticleManager(world, Minecraft.getMinecraft().getTextureManager());
    }
    public void addParticle(Particle particle) { if (particle != null) manager.addEffect(particle); }
    public void tick() { manager.updateEffects(); }
    public void renderParticles(Entity camera, float partialTicks) {
        if (camera != null) { manager.renderParticles(camera, partialTicks); manager.renderLitParticles(camera, partialTicks); }
    }
    public void clearEffects() { manager.clearEffects(null); }
}
