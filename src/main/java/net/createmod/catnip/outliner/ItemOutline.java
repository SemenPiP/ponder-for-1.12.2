package net.createmod.catnip.outliner;

import net.createmod.catnip.render.GlStateGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

public class ItemOutline extends Outline {
    private final Vec3d position;
    private final ItemStack stack;
    public ItemOutline(Vec3d position, ItemStack stack) { this.position=position; this.stack=stack.copy(); }
    @Override public void render(Vec3d camera,float partialTicks) {
        try(GlStateGuard ignored=GlStateGuard.capture()) {
            GlStateManager.pushMatrix(); GlStateManager.translate(position.x-camera.x,position.y-camera.y,position.z-camera.z);
            GlStateManager.scale(params.getAlpha(),params.getAlpha(),params.getAlpha());
            Minecraft.getMinecraft().getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.FIXED);
            GlStateManager.popMatrix();
        }
    }
}
