package net.createmod.ponder.foundation.structure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class PonderStructure {
    private final BlockPos size;
    private final List<BlockInfo> blocks;
    private final List<EntityInfo> entities;
    private final List<String> diagnostics;

    PonderStructure(BlockPos size, List<BlockInfo> blocks, List<EntityInfo> entities, List<String> diagnostics) {
        this.size = size.toImmutable();
        this.blocks = Collections.unmodifiableList(new ArrayList<BlockInfo>(blocks));
        this.entities = Collections.unmodifiableList(new ArrayList<EntityInfo>(entities));
        this.diagnostics = Collections.unmodifiableList(new ArrayList<String>(diagnostics));
    }

    public static PonderStructure missing(String diagnostic) {
        List<BlockInfo> blocks = new ArrayList<BlockInfo>();
        blocks.add(new BlockInfo(BlockPos.ORIGIN, net.minecraft.init.Blocks.BARRIER.getDefaultState(), null));
        return new PonderStructure(new BlockPos(1, 1, 1), blocks, Collections.<EntityInfo>emptyList(),
            Collections.singletonList(diagnostic));
    }

    public BlockPos getSize() { return size; }
    public List<BlockInfo> getBlocks() { return blocks; }
    public List<EntityInfo> getEntities() { return entities; }
    public List<String> getDiagnostics() { return diagnostics; }

    public void place(PonderLevel world) {
        if (world == null) throw new IllegalArgumentException("Ponder world is required");
        for (BlockInfo block : blocks)
            world.setBlockState(block.position, block.state, 2);
        for (BlockInfo block : blocks) {
            if (block.tileData == null) continue;
            NBTTagCompound data = block.tileData.copy();
            data.setInteger("x", block.position.getX());
            data.setInteger("y", block.position.getY());
            data.setInteger("z", block.position.getZ());
            TileEntity tile = TileEntity.create(world, data);
            if (tile != null) world.setTileEntity(block.position, tile);
        }
        for (EntityInfo info : entities) {
            NBTTagCompound data = info.data.copy();
            Entity entity = EntityList.createEntityFromNBT(data, world);
            if (entity == null) continue;
            entity.setPosition(info.position.x, info.position.y, info.position.z);
            world.spawnEntity(entity);
        }
    }

    public static final class BlockInfo {
        private final BlockPos position;
        private final IBlockState state;
        private final NBTTagCompound tileData;

        BlockInfo(BlockPos position, IBlockState state, NBTTagCompound tileData) {
            this.position = position.toImmutable();
            this.state = state;
            this.tileData = tileData == null ? null : tileData.copy();
        }

        public BlockPos getPosition() { return position; }
        public IBlockState getState() { return state; }
        public NBTTagCompound getTileData() { return tileData == null ? null : tileData.copy(); }
    }

    public static final class EntityInfo {
        private final Vec3d position;
        private final NBTTagCompound data;

        EntityInfo(Vec3d position, NBTTagCompound data) {
            this.position = position;
            this.data = data.copy();
        }

        public Vec3d getPosition() { return position; }
        public NBTTagCompound getData() { return data.copy(); }
    }
}
