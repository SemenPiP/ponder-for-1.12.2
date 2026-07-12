package net.createmod.ponder.api.scene;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public interface WorldInstructions {
    void incrementBlockBreakingProgress(BlockPos pos);
    void showSection(Selection selection, EnumFacing fadeInDirection);
    void showSectionAndMerge(Selection selection, EnumFacing fadeInDirection, ElementLink<WorldSectionElement> link);
    void glueBlockOnto(BlockPos position, EnumFacing fadeInDirection, ElementLink<WorldSectionElement> link);
    ElementLink<WorldSectionElement> showIndependentSection(Selection selection, EnumFacing fadeInDirection);
    ElementLink<WorldSectionElement> showIndependentSectionImmediately(Selection selection);
    void hideSection(Selection selection, EnumFacing fadeOutDirection);
    void hideIndependentSection(ElementLink<WorldSectionElement> link, EnumFacing fadeOutDirection);
    void restoreBlocks(Selection selection);
    ElementLink<WorldSectionElement> makeSectionIndependent(Selection selection);
    void rotateSection(ElementLink<WorldSectionElement> link, double xRotation, double yRotation,
                       double zRotation, int duration);
    void configureCenterOfRotation(ElementLink<WorldSectionElement> link, Vec3d anchor);
    void configureStabilization(ElementLink<WorldSectionElement> link, Vec3d anchor);
    void moveSection(ElementLink<WorldSectionElement> link, Vec3d offset, int duration);
    void setBlocks(Selection selection, IBlockState state, boolean spawnParticles);
    void destroyBlock(BlockPos pos);
    void setBlock(BlockPos pos, IBlockState state, boolean spawnParticles);
    void replaceBlocks(Selection selection, IBlockState state, boolean spawnParticles);
    void modifyBlock(BlockPos pos, UnaryOperator<IBlockState> stateFunction, boolean spawnParticles);
    void cycleBlockProperty(BlockPos pos, IProperty<?> property);
    void modifyBlocks(Selection selection, UnaryOperator<IBlockState> stateFunction, boolean spawnParticles);
    void toggleRedstonePower(Selection selection);
    <T extends Entity> void modifyEntities(Class<T> entityClass, Consumer<T> callback);
    <T extends Entity> void modifyEntitiesInside(Class<T> entityClass, Selection area, Consumer<T> callback);
    void modifyEntity(ElementLink<EntityElement> link, Consumer<Entity> callback);
    ElementLink<EntityElement> createEntity(Function<World, Entity> factory);
    ElementLink<EntityElement> createItemEntity(Vec3d location, Vec3d motion, ItemStack stack);
    void modifyBlockEntityNBT(Selection selection, Class<? extends TileEntity> type,
                              Consumer<NBTTagCompound> consumer);
    <T extends TileEntity> void modifyBlockEntity(BlockPos position, Class<T> type, Consumer<T> consumer);
    void modifyBlockEntityNBT(Selection selection, Class<? extends TileEntity> type,
                              Consumer<NBTTagCompound> consumer, boolean redrawBlocks);
}
