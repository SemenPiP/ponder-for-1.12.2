package net.createmod.ponder.foundation.structure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockRail;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

public class DemoStructureTest {
    private static final BlockPos SIZE = new BlockPos(5, 3, 5);
    private static ExpectedStructure[] structures;

    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
        structures = new ExpectedStructure[] {
            expected("basics", new BlockPos(2, 1, 2), Blocks.CRAFTING_TABLE, 1),
            expected("storage", new BlockPos(2, 1, 2), Blocks.CHEST, 0),
            expected("smelting", new BlockPos(2, 1, 2), Blocks.FURNACE, 0),
            expected("piston", new BlockPos(1, 1, 2), Blocks.PISTON, 0),
            expected("redstone", new BlockPos(4, 1, 2), Blocks.REDSTONE_LAMP, 0),
            expected("render_layers", new BlockPos(2, 1, 2), Blocks.GLASS, 0),
            expected("fluids", new BlockPos(2, 1, 2), Blocks.WATER, 0),
            expected("rail", new BlockPos(1, 1, 1), Blocks.RAIL, 0)
        };
    }

    @Test
    public void allBundledDemosLoadWithoutDiagnosticsOrBarrierFallbacks() throws Exception {
        Set<String> layouts = new HashSet<String>();
        for (ExpectedStructure expected : structures) {
            PonderStructure structure = load(expected.name);
            String label = "demo/" + expected.name;
            assertEquals(label, SIZE, structure.getSize());
            assertFalse(label + " has no blocks", structure.getBlocks().isEmpty());
            assertTrue(label + " diagnostics: " + structure.getDiagnostics(), structure.getDiagnostics().isEmpty());
            assertFalse(label + " contains a barrier fallback", hasBlock(structure, Blocks.BARRIER));
            assertEquals(label + " entity count", expected.entityCount, structure.getEntities().size());
            assertEquals(label + " target", expected.targetBlock, stateAt(structure, expected.targetPos).getBlock());
            assertTrue(label + " must include a complete 5x5 base", blocksAtY(structure, 0) >= 25);
            assertTrue(label + " reused another demo layout", layouts.add(signature(structure)));
        }
        assertEquals(8, layouts.size());
    }

    @Test
    public void basicsCoversEveryVanillaRenderLayerAndKeepsItsEntity() throws Exception {
        PonderStructure structure = load("basics");
        assertEquals(34, structure.getBlocks().size());
        assertTrue(hasBlock(structure, Blocks.STONE));
        assertTrue(hasBlock(structure, Blocks.GLASS));
        assertTrue(hasBlock(structure, Blocks.WATER));
        assertTrue(hasBlock(structure, Blocks.CHEST));
        assertTrue(hasBlock(structure, Blocks.TORCH));
        assertTrue(hasBlock(structure, Blocks.LEAVES));
        assertTrue(hasBlock(structure, Blocks.GRASS));
        assertTrue(hasLayer(structure, BlockRenderLayer.SOLID));
        assertTrue(hasLayer(structure, BlockRenderLayer.CUTOUT));
        assertTrue(hasLayer(structure, BlockRenderLayer.CUTOUT_MIPPED));
        assertTrue(hasLayer(structure, BlockRenderLayer.TRANSLUCENT));

        PonderLevel world = place(structure);
        TileEntityChest chest = requireChest(world, new BlockPos(3, 1, 2));
        assertEquals(Items.BOOK, chest.getStackInSlot(0).getItem());
        assertEquals(1, world.getEntities().size());
        assertTrue(world.getEntities().iterator().next() instanceof EntityArmorStand);
    }

    @Test
    public void storageAndSmeltingCarryUsableTileEntityData() throws Exception {
        PonderLevel storage = place(load("storage"));
        TileEntityChest chest = requireChest(storage, new BlockPos(2, 1, 2));
        assertFalse(chest.hasCustomName());
        assertEquals(Items.BOOK, chest.getStackInSlot(0).getItem());
        assertEquals(4, chest.getStackInSlot(0).getCount());
        assertEquals(Items.COMPASS, chest.getStackInSlot(1).getItem());
        assertEquals(Blocks.CHEST, Block.getBlockFromItem(chest.getStackInSlot(8).getItem()));

        PonderLevel smelting = place(load("smelting"));
        assertTrue(smelting.getTileEntity(new BlockPos(2, 1, 2)) instanceof TileEntityFurnace);
        TileEntityFurnace furnace = (TileEntityFurnace) smelting.getTileEntity(new BlockPos(2, 1, 2));
        assertEquals("Ponder Furnace", furnace.getName());
        assertTrue(furnace.getStackInSlot(0).isEmpty());
        assertTrue(furnace.getStackInSlot(1).isEmpty());
        assertTrue(furnace.getStackInSlot(2).isEmpty());
        assertEquals(0, furnace.getField(0));
        assertEquals(0, furnace.getField(1));
        assertEquals(0, furnace.getField(2));
        assertEquals(200, furnace.getField(3));
    }

    @Test
    public void mechanicalRedstoneFluidAndRailStatesMatchSceneCoordinates() throws Exception {
        PonderStructure piston = load("piston");
        IBlockState pistonState = stateAt(piston, new BlockPos(1, 1, 2));
        assertEquals(EnumFacing.EAST, pistonState.getValue(BlockPistonBase.FACING));
        assertFalse(pistonState.getValue(BlockPistonBase.EXTENDED));
        assertEquals(Blocks.SLIME_BLOCK, stateAt(piston, new BlockPos(2, 1, 2)).getBlock());
        assertEquals(Blocks.OBSIDIAN, stateAt(piston, new BlockPos(4, 1, 2)).getBlock());

        PonderStructure redstone = load("redstone");
        assertEquals(Blocks.REDSTONE_BLOCK, stateAt(redstone, new BlockPos(1, 1, 2)).getBlock());
        assertEquals(Integer.valueOf(0), stateAt(redstone, new BlockPos(2, 1, 2)).getValue(BlockRedstoneWire.POWER));
        assertEquals(Blocks.REDSTONE_LAMP, stateAt(redstone, new BlockPos(4, 1, 2)).getBlock());

        PonderStructure fluids = load("fluids");
        assertEquals(Blocks.WATER, stateAt(fluids, new BlockPos(2, 1, 2)).getBlock());
        assertEquals(Blocks.FLOWING_WATER, stateAt(fluids, new BlockPos(1, 1, 2)).getBlock());
        assertEquals(Blocks.FLOWING_WATER, stateAt(fluids, new BlockPos(3, 1, 2)).getBlock());
        assertEquals(Integer.valueOf(0), stateAt(fluids, new BlockPos(2, 1, 2)).getValue(BlockLiquid.LEVEL));
        assertEquals(Integer.valueOf(4), stateAt(fluids, new BlockPos(1, 1, 2)).getValue(BlockLiquid.LEVEL));
        assertEquals(Integer.valueOf(7), stateAt(fluids, new BlockPos(3, 1, 2)).getValue(BlockLiquid.LEVEL));

        PonderStructure rail = load("rail");
        assertEquals(BlockRailBase.EnumRailDirection.NORTH_SOUTH,
            stateAt(rail, new BlockPos(1, 1, 1)).getValue(BlockRail.SHAPE));
        assertEquals(BlockRailBase.EnumRailDirection.NORTH_EAST,
            stateAt(rail, new BlockPos(1, 1, 3)).getValue(BlockRail.SHAPE));
        assertEquals(BlockRailBase.EnumRailDirection.EAST_WEST,
            stateAt(rail, new BlockPos(3, 1, 3)).getValue(BlockRail.SHAPE));
    }

    @Test
    public void renderLayerDemoHasOneExplicitExampleForEveryLayer() throws Exception {
        PonderStructure structure = load("render_layers");
        assertEquals(Blocks.STONE, stateAt(structure, new BlockPos(1, 1, 2)).getBlock());
        assertEquals(Blocks.GLASS, stateAt(structure, new BlockPos(2, 1, 2)).getBlock());
        assertEquals(Blocks.GRASS, stateAt(structure, new BlockPos(3, 1, 2)).getBlock());
        assertEquals(Blocks.STAINED_GLASS, stateAt(structure, new BlockPos(4, 1, 2)).getBlock());
        assertTrue(hasLayerAt(structure, new BlockPos(1, 1, 2), BlockRenderLayer.SOLID));
        assertTrue(hasLayerAt(structure, new BlockPos(2, 1, 2), BlockRenderLayer.CUTOUT));
        assertTrue(hasLayerAt(structure, new BlockPos(3, 1, 2), BlockRenderLayer.CUTOUT_MIPPED));
        assertTrue(hasLayerAt(structure, new BlockPos(4, 1, 2), BlockRenderLayer.TRANSLUCENT));
    }

    private static ExpectedStructure expected(String name, BlockPos targetPos, Block targetBlock, int entityCount) {
        return new ExpectedStructure(name, targetPos, targetBlock, entityCount);
    }

    private static PonderStructure load(String name) throws Exception {
        return new PonderStructureLoader().load(new ResourceLocation("ponder", "demo/" + name));
    }

    private static PonderLevel place(PonderStructure structure) {
        PonderLevel world = new PonderLevel(BlockPos.ORIGIN, null);
        structure.place(world);
        return world;
    }

    private static TileEntityChest requireChest(PonderLevel world, BlockPos pos) {
        assertTrue(world.getTileEntity(pos) instanceof TileEntityChest);
        return (TileEntityChest) world.getTileEntity(pos);
    }

    private static IBlockState stateAt(PonderStructure structure, BlockPos pos) {
        for (PonderStructure.BlockInfo info : structure.getBlocks())
            if (info.getPosition().equals(pos)) return info.getState();
        throw new AssertionError("No block at " + pos);
    }

    private static int blocksAtY(PonderStructure structure, int y) {
        int count = 0;
        for (PonderStructure.BlockInfo info : structure.getBlocks())
            if (info.getPosition().getY() == y) count++;
        return count;
    }

    private static boolean hasBlock(PonderStructure structure, Block block) {
        for (PonderStructure.BlockInfo info : structure.getBlocks())
            if (info.getState().getBlock() == block) return true;
        return false;
    }

    private static boolean hasLayer(PonderStructure structure, BlockRenderLayer layer) {
        for (PonderStructure.BlockInfo info : structure.getBlocks())
            if (info.getState().getBlock().canRenderInLayer(info.getState(), layer)) return true;
        return false;
    }

    private static boolean hasLayerAt(PonderStructure structure, BlockPos pos, BlockRenderLayer layer) {
        IBlockState state = stateAt(structure, pos);
        return state.getBlock().canRenderInLayer(state, layer);
    }

    private static String signature(PonderStructure structure) {
        StringBuilder signature = new StringBuilder();
        for (PonderStructure.BlockInfo info : structure.getBlocks()) {
            signature.append(info.getPosition()).append('=')
                .append(info.getState().toString()).append(';');
        }
        return signature.toString();
    }

    private static final class ExpectedStructure {
        final String name;
        final BlockPos targetPos;
        final Block targetBlock;
        final int entityCount;

        ExpectedStructure(String name, BlockPos targetPos, Block targetBlock, int entityCount) {
            this.name = name;
            this.targetPos = targetPos;
            this.targetBlock = targetBlock;
            this.entityCount = entityCount;
        }
    }
}
