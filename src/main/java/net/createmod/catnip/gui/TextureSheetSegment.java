package net.createmod.catnip.gui;

import net.minecraft.util.ResourceLocation;

public interface TextureSheetSegment {
    ResourceLocation getTextureLocation();
    int getStartX();
    int getStartY();
    int getWidth();
    int getHeight();
    int getSheetWidth();
    int getSheetHeight();
}
