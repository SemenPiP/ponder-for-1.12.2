package net.createmod.catnip.render;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

public interface BindableTexture {
    ResourceLocation getLocation();

    default void bind() {
        Minecraft.getMinecraft().getTextureManager().bindTexture(getLocation());
    }
}
