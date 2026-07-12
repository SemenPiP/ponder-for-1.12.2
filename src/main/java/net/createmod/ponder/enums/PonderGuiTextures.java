package net.createmod.ponder.enums;

import net.createmod.catnip.gui.TextureSheetSegment;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.gui.element.ScreenElement;
import net.createmod.catnip.theme.Color;
import net.createmod.ponder.Ponder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public enum PonderGuiTextures implements TextureSheetSegment, ScreenElement {
    LOGO("logo", 0, 0, 32, 32, 32, 32),
    SPEECH_TOOLTIP_BACKGROUND("widgets", 0, 24, 8, 8),
    SPEECH_TOOLTIP_COLOR("widgets", 8, 24, 8, 8),
    ICON_PONDER_LEFT("widgets", 0, 2), ICON_PONDER_CLOSE("widgets", 1, 2),
    ICON_PONDER_RIGHT("widgets", 2, 2), ICON_PONDER_IDENTIFY("widgets", 3, 2),
    ICON_PONDER_REPLAY("widgets", 4, 2), ICON_PONDER_USER_MODE("widgets", 5, 2),
    ICON_PONDER_SLOW_MODE("widgets", 6, 2), ICON_CONFIG_UNLOCKED("widgets", 0, 3),
    ICON_CONFIG_LOCKED("widgets", 1, 3), ICON_CONFIG_DISCARD("widgets", 2, 3),
    ICON_CONFIG_SAVE("widgets", 3, 3), ICON_CONFIG_RESET("widgets", 4, 3),
    ICON_CONFIG_BACK("widgets", 5, 3), ICON_CONFIG_PREV("widgets", 6, 3),
    ICON_CONFIG_NEXT("widgets", 7, 3), ICON_DISABLE("widgets", 8, 3),
    ICON_CONFIG_OPEN("widgets", 9, 3), ICON_CONFIRM("widgets", 10, 3),
    ICON_LMB("widgets", 0, 4), ICON_SCROLL("widgets", 1, 4), ICON_RMB("widgets", 2, 4),
    PLACEMENT_INDICATOR_SHEET("placement_indicator", 0, 0, 16, 256);

    private final ResourceLocation location;
    private final int startX, startY, width, height, sheetWidth, sheetHeight;

    PonderGuiTextures(String file, int column, int row) { this(file, column * 16, row * 16, 16, 16); }
    PonderGuiTextures(String file, int x, int y, int width, int height) { this(file, x, y, width, height, 256, 256); }
    PonderGuiTextures(String file, int x, int y, int width, int height, int sheetWidth, int sheetHeight) {
        location = new ResourceLocation(Ponder.MOD_ID, "textures/gui/" + file + ".png");
        startX = x; startY = y; this.width = width; this.height = height;
        this.sheetWidth = sheetWidth; this.sheetHeight = sheetHeight;
    }

    @Override public void render(int x, int y) { UIRenderHelper.texturedQuad(this, x, y); }
    public void render(int x, int y, Color color) {
        GlStateManager.color(color.getRedAsFloat(), color.getGreenAsFloat(), color.getBlueAsFloat(), color.getAlphaAsFloat());
        UIRenderHelper.texturedQuad(this, x, y);
        UIRenderHelper.resetColor();
    }
    public ResourceLocation getLocation() { return location; }
    @Override public ResourceLocation getTextureLocation() { return location; }
    @Override public int getStartX() { return startX; }
    @Override public int getStartY() { return startY; }
    @Override public int getWidth() { return width; }
    @Override public int getHeight() { return height; }
    @Override public int getSheetWidth() { return sheetWidth; }
    @Override public int getSheetHeight() { return sheetHeight; }
}
