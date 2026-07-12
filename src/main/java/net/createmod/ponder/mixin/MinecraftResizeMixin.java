package net.createmod.ponder.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.createmod.ponder.client.PonderClientEvents;
import net.minecraft.client.Minecraft;

@Mixin(Minecraft.class)
public abstract class MinecraftResizeMixin {
    @Inject(method = "resize", at = @At("RETURN"))
    private void ponder$afterResize(int width, int height, CallbackInfo callback) {
        PonderClientEvents.onWindowResize(Math.max(1, width), Math.max(1, height));
    }
}
