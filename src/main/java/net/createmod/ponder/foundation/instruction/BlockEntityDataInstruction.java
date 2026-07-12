package net.createmod.ponder.foundation.instruction;

import java.util.function.Consumer;

import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

public final class BlockEntityDataInstruction extends CallbackInstruction {
    public BlockEntityDataInstruction(final Selection selection, final Class<? extends TileEntity> type,
                                      final Consumer<NBTTagCompound> consumer, final boolean redraw) {
        super(scene -> {
            if (scene.getWorld() == null) return;
            for (BlockPos pos : selection) {
                TileEntity tile = scene.getWorld().getTileEntity(pos);
                if (!type.isInstance(tile)) continue;
                NBTTagCompound data = tile.writeToNBT(new NBTTagCompound());
                consumer.accept(data);
                data.setInteger("x", pos.getX()); data.setInteger("y", pos.getY()); data.setInteger("z", pos.getZ());
                tile.readFromNBT(data);
                tile.markDirty();
            }
            if (redraw) scene.forEach(WorldSectionElement.class, WorldSectionElement::queueRedraw);
        });
    }
}
