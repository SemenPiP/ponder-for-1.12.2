package net.createmod.ponder.api.scene;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.InputElementBuilder;
import net.createmod.ponder.api.element.TextElementBuilder;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public interface OverlayInstructions {
    TextElementBuilder showText(int duration);
    TextElementBuilder showOutlineWithText(Selection selection, int duration);
    InputElementBuilder showControls(Vec3d sceneSpace, Pointing direction, int duration);
    void chaseBoundingBoxOutline(PonderPalette color, Object slot, AxisAlignedBB boundingBox, int duration);
    void showCenteredScrollInput(BlockPos pos, EnumFacing side, int duration);
    void showScrollInput(Vec3d location, EnumFacing side, int duration);
    void showRepeaterScrollInput(BlockPos pos, int duration);
    void showFilterSlotInput(Vec3d location, int duration);
    void showFilterSlotInput(Vec3d location, EnumFacing side, int duration);
    void showLine(PonderPalette color, Vec3d start, Vec3d end, int duration);
    void showBigLine(PonderPalette color, Vec3d start, Vec3d end, int duration);
    void showOutline(PonderPalette color, Object slot, Selection selection, int duration);
}
