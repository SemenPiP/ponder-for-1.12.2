package net.createmod.catnip.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;

public class StitchedSprite {
    private static final List<StitchedSprite> ALL=Collections.synchronizedList(new ArrayList<StitchedSprite>());
    private final ResourceLocation atlas;
    private final ResourceLocation location;
    private TextureAtlasSprite sprite;
    public StitchedSprite(ResourceLocation location){this(TextureMap.LOCATION_BLOCKS_TEXTURE,location);}
    public StitchedSprite(ResourceLocation atlas,ResourceLocation location){this.atlas=atlas;this.location=location;ALL.add(this);}
    public ResourceLocation getAtlasLocation(){return atlas;}
    public ResourceLocation getLocation(){return location;}
    public TextureAtlasSprite get(){if(sprite==null)sprite=Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(location.toString());return sprite;}
    public static void onTextureStitchPre(TextureStitchEvent.Pre event){if(!TextureMap.LOCATION_BLOCKS_TEXTURE.equals(event.getMap().getBasePath()==null?TextureMap.LOCATION_BLOCKS_TEXTURE:TextureMap.LOCATION_BLOCKS_TEXTURE))return;synchronized(ALL){for(StitchedSprite sprite:ALL)event.getMap().registerSprite(sprite.location);}}
    public static void onTextureStitchPost(TextureStitchEvent.Post event){synchronized(ALL){for(StitchedSprite holder:ALL)holder.sprite=event.getMap().getAtlasSprite(holder.location.toString());}}
}
