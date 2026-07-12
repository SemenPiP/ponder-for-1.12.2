package net.createmod.ponder.foundation.instruction;

import net.createmod.ponder.api.element.MinecartElement;
import net.minecraft.util.EnumFacing;

public final class CreateMinecartInstruction extends FadeIntoSceneInstruction<MinecartElement> {
    public CreateMinecartInstruction(int ticks, EnumFacing direction, MinecartElement element) { super(ticks, direction, element); }
    @Override protected Class<MinecartElement> getElementClass() { return MinecartElement.class; }
}
