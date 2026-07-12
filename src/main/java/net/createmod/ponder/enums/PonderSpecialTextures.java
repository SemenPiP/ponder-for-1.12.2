package net.createmod.ponder.enums;

import net.createmod.catnip.render.BindableTexture;
import net.createmod.ponder.Ponder;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public enum PonderSpecialTextures implements BindableTexture {
    BLANK("blank.png");

    public static final String ASSET_PATH = "textures/special/";
    private final ResourceLocation location;

    PonderSpecialTextures(String file) { location = Ponder.asResource(ASSET_PATH + file); }
    @Override public ResourceLocation getLocation() { return location; }
}
