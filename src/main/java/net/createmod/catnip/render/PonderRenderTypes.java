package net.createmod.catnip.render;

import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ResourceLocation;

/** Fixed-pipeline equivalents of the modern named render types. */
public final class PonderRenderTypes {
    private PonderRenderTypes(){}
    public static BlockRenderLayer outlineSolid(){return BlockRenderLayer.SOLID;}
    public static BlockRenderLayer outlineTranslucent(ResourceLocation texture,boolean cull){return BlockRenderLayer.TRANSLUCENT;}
    public static BlockRenderLayer fluid(){return BlockRenderLayer.TRANSLUCENT;}
}
