package net.createmod.ponder.foundation.instruction;

import net.createmod.ponder.api.element.ParrotElement;
import net.minecraft.util.EnumFacing;

public final class CreateParrotInstruction extends FadeIntoSceneInstruction<ParrotElement> {
    public CreateParrotInstruction(int ticks, EnumFacing direction, ParrotElement element) { super(ticks, direction, element); }
    @Override protected Class<ParrotElement> getElementClass() { return ParrotElement.class; }
}
