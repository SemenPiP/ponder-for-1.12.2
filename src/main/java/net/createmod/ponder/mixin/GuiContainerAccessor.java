package net.createmod.ponder.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;

@Mixin(GuiContainer.class)
public interface GuiContainerAccessor {
    @Accessor("hoveredSlot")
    Slot ponder$getHoveredSlot();
}
