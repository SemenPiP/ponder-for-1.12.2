package net.createmod.catnip.render;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;

public class SpriteShiftEntry {
    protected StitchedSprite original;
    protected StitchedSprite target;
    private float scrollU;
    private float scrollV;
    public void set(ResourceLocation originalLocation,ResourceLocation targetLocation){original=new StitchedSprite(originalLocation);target=new StitchedSprite(targetLocation);}
    public ResourceLocation getOriginalResourceLocation(){return require(original).getLocation();}
    public ResourceLocation getTargetResourceLocation(){return require(target).getLocation();}
    public TextureAtlasSprite getOriginal(){return require(original).get();}
    public TextureAtlasSprite getTarget(){return require(target).get();}
    public float getTargetU(float u){return getTarget().getInterpolatedU((getUnInterpolatedU(getOriginal(),u)+scrollU)*16);}
    public float getTargetV(float v){return getTarget().getInterpolatedV((getUnInterpolatedV(getOriginal(),v)+scrollV)*16);}
    public float[] shift(float u,float v){return new float[]{getTargetU(u),getTargetV(v)};}
    SpriteShiftEntry scrolled(float u,float v){SpriteShiftEntry shifted=new SpriteShiftEntry();shifted.original=original;shifted.target=target;shifted.scrollU=u;shifted.scrollV=v;return shifted;}
    public static float getUnInterpolatedU(TextureAtlasSprite sprite,float u){float width=sprite.getMaxU()-sprite.getMinU();return width==0?0:(u-sprite.getMinU())/width;}
    public static float getUnInterpolatedV(TextureAtlasSprite sprite,float v){float height=sprite.getMaxV()-sprite.getMinV();return height==0?0:(v-sprite.getMinV())/height;}
    private static StitchedSprite require(StitchedSprite sprite){if(sprite==null)throw new IllegalStateException("Sprite shift was not initialized");return sprite;}
}
