package net.createmod.catnip.levelWrappers;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/** Mutable, isolated block view used by schematic and placement previews. */
public interface SchematicLevelAccessor extends IBlockAccess {
    Set<BlockPos> getAllPositions();
    List<Entity> getEntityList();
    Map<BlockPos, IBlockState> getBlockMap();
    AxisAlignedBB getBounds();
    void setBounds(AxisAlignedBB bounds);
    Iterable<TileEntity> getBlockEntities();
    Iterable<TileEntity> getRenderedBlockEntities();
    boolean setBlockState(BlockPos pos, IBlockState state, int flags);
    void setTileEntity(BlockPos pos, TileEntity tileEntity);
}
