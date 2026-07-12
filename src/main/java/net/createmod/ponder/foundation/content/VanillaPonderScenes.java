package net.createmod.ponder.foundation.content;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.element.MinecartElement;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.instruction.StaggeredDisplayWorldSectionInstruction;
import net.minecraft.block.BlockFurnace;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityMinecartEmpty;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

final class VanillaPonderScenes {
    private static final int STEP_TICKS = 150;
    private static final int TEXT_TICKS = 120;

    private VanillaPonderScenes() {}

    static void registerAll(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.addStoryBoard(component("crafting_table"), "demo/basics",
            VanillaPonderScenes::ponderBasics, BasePonderPlugin.BASICS);
        helper.addStoryBoard(component("chest"), "demo/storage",
            VanillaPonderScenes::chestStorage, BasePonderPlugin.STORAGE);
        helper.addStoryBoard(component("furnace"), "demo/smelting",
            VanillaPonderScenes::furnaceSmelting, BasePonderPlugin.MECHANICS);
        helper.addStoryBoard(component("piston"), "demo/piston",
            VanillaPonderScenes::pistonMovement, BasePonderPlugin.MECHANICS);
        helper.addStoryBoard(component("redstone_lamp"), "demo/redstone",
            VanillaPonderScenes::redstoneLampPower, BasePonderPlugin.REDSTONE);
        helper.addStoryBoard(component("glass"), "demo/render_layers",
            VanillaPonderScenes::glassRenderLayers, BasePonderPlugin.RENDERING);
        helper.addStoryBoard(component("water_bucket"), "demo/fluids",
            VanillaPonderScenes::waterHandling, BasePonderPlugin.RENDERING);
        helper.addStoryBoard(component("rail"), "demo/rail",
            VanillaPonderScenes::railMinecart, BasePonderPlugin.MECHANICS);
    }

    static void ponderBasics(SceneBuilder scene, SceneBuildingUtil util) {
        setup(scene, "ponder_basics", "Ponder for Minecraft 1.12.2", util, util.grid().at(2, 1, 2));
        BlockPos crafting = util.grid().at(2, 1, 2);
        BlockPos chest = util.grid().at(3, 1, 2);

        scene.overlay().showText(TEXT_TICKS)
            .text("Ponder scenes reveal a prepared structure one layer at a time.")
            .pointAt(util.vector().centerOf(crafting))
            .placeNearTarget()
            .attachKeyFrame();
        scene.overlay().showOutline(PonderPalette.INPUT, "basics_crafting",
            util.select().position(crafting), TEXT_TICKS);
        scene.world().modifyBlockEntity(chest, TileEntityChest.class,
            tile -> tile.setInventorySlotContents(1, new ItemStack(Items.COMPASS)));
        scene.world().modifyEntities(EntityArmorStand.class, entity -> entity.setGlowing(true));
        scene.idle(STEP_TICKS);

        ElementLink<WorldSectionElement> chestSection = scene.world().makeSectionIndependent(
            util.select().position(chest));
        scene.world().configureCenterOfRotation(chestSection, util.vector().centerOf(chest));
        scene.world().configureStabilization(chestSection, util.vector().centerOf(chest));
        scene.world().moveSection(chestSection, new Vec3d(0, 1, 0), 55);
        scene.world().rotateSection(chestSection, 0, 90, 0, 55);
        scene.effects().emitParticles(util.vector().topOf(chest),
            scene.effects().simpleParticleEmitter(EnumParticleTypes.VILLAGER_HAPPY, new Vec3d(0, .04, 0)),
            1, 35);
        scene.overlay().showText(TEXT_TICKS)
            .text("Blocks can become independent sections that move and rotate without changing the source structure.")
            .pointAt(util.vector().topOf(chest))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(STEP_TICKS);

        ElementLink<EntityElement> book = scene.world().createItemEntity(
            new Vec3d(2.5, 2.2, 2.5), Vec3d.ZERO, new ItemStack(Items.BOOK));
        scene.world().modifyEntity(book, entity -> {
            entity.setNoGravity(true);
            entity.setGlowing(true);
        });
        scene.effects().indicateSuccess(crafting);
        scene.rotateCameraY(35);
        scene.overlay().showText(TEXT_TICKS)
            .text("Overlays, particles and virtual entities call attention to each part of the explanation.")
            .pointAt(util.vector().topOf(crafting))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(STEP_TICKS);

        scene.world().moveSection(chestSection, new Vec3d(0, -1, 0), 55);
        scene.world().rotateSection(chestSection, 0, -90, 0, 55);
        scene.overlay().showText(TEXT_TICKS)
            .text("Keyframes make replaying and seeking deterministic, so every step can be inspected again.")
            .pointAt(util.vector().centerOf(chest))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(STEP_TICKS);
        scene.markAsFinished();
    }

    static void chestStorage(SceneBuilder scene, SceneBuildingUtil util) {
        setup(scene, "chest_storage", "Storing Items in a Chest", util, util.grid().at(2, 1, 2));
        BlockPos chest = util.grid().at(2, 1, 2);

        scene.overlay().showText(TEXT_TICKS)
            .text("Right-click a chest in the world to open its 27 inventory slots.")
            .pointAt(util.vector().centerOf(chest))
            .placeNearTarget()
            .attachKeyFrame();
        scene.overlay().showControls(util.vector().topOf(chest), Pointing.DOWN, 80).rightClick();
        scene.overlay().showOutline(PonderPalette.INPUT, "storage_chest",
            util.select().position(chest), TEXT_TICKS);
        scene.idle(STEP_TICKS);

        scene.world().modifyBlockEntity(chest, TileEntityChest.class, tile -> {
            tile.setInventorySlotContents(0, new ItemStack(Items.BOOK));
            tile.setInventorySlotContents(1, new ItemStack(Items.COMPASS));
        });
        ElementLink<EntityElement> storedItem = scene.world().createItemEntity(
            util.vector().topOf(chest), Vec3d.ZERO, new ItemStack(Items.BOOK));
        scene.world().modifyEntity(storedItem, entity -> {
            entity.setNoGravity(true);
            entity.setGlowing(true);
        });
        scene.overlay().showText(TEXT_TICKS)
            .text("Scene scripts can change inventory slots and display matching item entities above the block.")
            .pointAt(util.vector().topOf(chest))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(STEP_TICKS);

        scene.world().modifyBlockEntityNBT(util.select().position(chest), TileEntityChest.class,
            nbt -> nbt.setString("CustomName", "Ponder Storage"), true);
        scene.overlay().showText(TEXT_TICKS)
            .text("Block entity NBT can also be edited explicitly, including a chest's custom display name.")
            .pointAt(util.vector().centerOf(chest))
            .placeNearTarget()
            .attachKeyFrame();
        scene.effects().emitParticles(util.vector().topOf(chest),
            scene.effects().simpleParticleEmitter(EnumParticleTypes.ENCHANTMENT_TABLE, new Vec3d(0, .03, 0)),
            1, 45);
        scene.idle(STEP_TICKS);

        scene.world().modifyBlockEntity(chest, TileEntityChest.class,
            tile -> tile.setInventorySlotContents(2, new ItemStack(Items.CLOCK)));
        scene.effects().indicateSuccess(chest);
        scene.overlay().showText(TEXT_TICKS)
            .text("Inventory and NBT changes are restored from the structure whenever this scene is replayed.")
            .pointAt(util.vector().centerOf(chest))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(STEP_TICKS);
        scene.markAsFinished();
    }

    static void furnaceSmelting(SceneBuilder scene, SceneBuildingUtil util) {
        setup(scene, "furnace_smelting", "Smelting with a Furnace", util, util.grid().at(2, 1, 2));
        BlockPos furnace = util.grid().at(2, 1, 2);
        BlockPos ore = util.grid().at(1, 1, 2);
        BlockPos fuel = util.grid().at(3, 1, 2);

        scene.overlay().showText(TEXT_TICKS)
            .text("A furnace combines an input item with fuel to produce a smelted output.")
            .pointAt(util.vector().centerOf(furnace))
            .placeNearTarget()
            .attachKeyFrame();
        scene.overlay().showOutline(PonderPalette.INPUT, "smelting_inputs",
            util.select().position(ore).add(util.select().position(fuel)), TEXT_TICKS);
        scene.world().createItemEntity(util.vector().topOf(ore), Vec3d.ZERO, new ItemStack(Blocks.IRON_ORE));
        scene.world().createItemEntity(util.vector().topOf(fuel), Vec3d.ZERO, new ItemStack(Items.COAL));
        scene.idle(STEP_TICKS);

        scene.world().modifyBlock(furnace, state -> Blocks.LIT_FURNACE.getDefaultState()
            .withProperty(BlockFurnace.FACING, state.getValue(BlockFurnace.FACING)), true);
        scene.world().modifyBlockEntity(furnace, TileEntityFurnace.class, tile -> {
            tile.setInventorySlotContents(0, new ItemStack(Blocks.IRON_ORE));
            tile.setInventorySlotContents(1, new ItemStack(Items.COAL));
            tile.setField(0, 160);
            tile.setField(1, 160);
        });
        scene.overlay().showText(TEXT_TICKS)
            .text("The demonstration explicitly lights the furnace and fills its input and fuel slots.")
            .pointAt(util.vector().centerOf(furnace))
            .placeNearTarget()
            .attachKeyFrame();
        scene.effects().emitParticles(util.vector().topOf(furnace),
            scene.effects().simpleParticleEmitter(EnumParticleTypes.FLAME, new Vec3d(0, .035, 0)),
            1, 55);
        scene.idle(STEP_TICKS);

        scene.world().modifyBlockEntity(furnace, TileEntityFurnace.class, tile -> {
            tile.setField(2, 100);
            tile.setField(3, 200);
        });
        scene.overlay().showText(TEXT_TICKS)
            .text("Cook progress is scripted here for a clear timeline; the virtual world does not simulate a live furnace recipe.")
            .pointAt(util.vector().topOf(furnace))
            .placeNearTarget()
            .attachKeyFrame();
        scene.effects().emitParticles(util.vector().topOf(furnace),
            scene.effects().simpleParticleEmitter(EnumParticleTypes.SMOKE_NORMAL, new Vec3d(0, .025, 0)),
            1, 65);
        scene.idle(STEP_TICKS);

        scene.world().modifyBlockEntity(furnace, TileEntityFurnace.class, tile -> {
            tile.setInventorySlotContents(0, ItemStack.EMPTY);
            tile.setInventorySlotContents(2, new ItemStack(Items.IRON_INGOT));
            tile.setField(2, 200);
        });
        scene.world().createItemEntity(util.vector().topOf(furnace), new Vec3d(0, .04, 0),
            new ItemStack(Items.IRON_INGOT));
        scene.effects().indicateSuccess(furnace);
        scene.overlay().showText(TEXT_TICKS)
            .text("When the scripted progress completes, the iron ingot appears in the output slot.")
            .pointAt(util.vector().topOf(furnace))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(STEP_TICKS);
        scene.markAsFinished();
    }

    static void pistonMovement(SceneBuilder scene, SceneBuildingUtil util) {
        setup(scene, "piston_movement", "Moving Blocks with a Piston", util, util.grid().at(1, 1, 2));
        BlockPos piston = util.grid().at(1, 1, 2);
        BlockPos movable = util.grid().at(2, 1, 2);
        BlockPos travel = util.grid().at(3, 1, 2);
        BlockPos anchor = util.grid().at(4, 1, 2);

        scene.overlay().showText(TEXT_TICKS)
            .text("This piston faces east toward a slime block with one block of travel space.")
            .pointAt(util.vector().centerOf(piston))
            .placeNearTarget()
            .attachKeyFrame();
        scene.overlay().showControls(util.vector().topOf(piston), Pointing.DOWN, 80)
            .rightClick().withItem(new ItemStack(Items.REDSTONE));
        scene.overlay().showOutline(PonderPalette.INPUT, "piston_input",
            util.select().position(piston), TEXT_TICKS);
        scene.idle(STEP_TICKS);

        ElementLink<WorldSectionElement> moving = scene.world().makeSectionIndependent(
            util.select().position(movable));
        scene.world().configureCenterOfRotation(moving, util.vector().centerOf(movable));
        scene.world().configureStabilization(moving, util.vector().centerOf(anchor));
        scene.world().modifyBlock(piston,
            state -> state.withProperty(BlockPistonBase.EXTENDED, Boolean.TRUE), true);
        scene.world().moveSection(moving, new Vec3d(1, 0, 0), 65);
        scene.overlay().showLine(PonderPalette.OUTPUT, util.vector().centerOf(movable),
            util.vector().centerOf(travel), TEXT_TICKS);
        scene.overlay().showText(TEXT_TICKS)
            .text("The slime block becomes an independent section and moves into the marked travel position.")
            .pointAt(util.vector().centerOf(travel))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(STEP_TICKS);

        scene.overlay().showText(TEXT_TICKS)
            .text("Independent sections can also rotate while a stabilization anchor remains fixed.")
            .pointAt(util.vector().centerOf(anchor))
            .placeNearTarget()
            .attachKeyFrame();
        scene.overlay().showOutline(PonderPalette.BLUE, "piston_anchor",
            util.select().position(anchor), TEXT_TICKS);
        scene.world().rotateSection(moving, 0, 90, 0, 60);
        scene.idle(70);
        scene.world().rotateSection(moving, 0, -90, 0, 60);
        scene.idle(STEP_TICKS - 70);

        scene.world().modifyBlock(piston,
            state -> state.withProperty(BlockPistonBase.EXTENDED, Boolean.FALSE), true);
        scene.world().moveSection(moving, new Vec3d(-1, 0, 0), 55);
        scene.world().hideIndependentSection(moving, EnumFacing.UP);
        scene.overlay().showText(TEXT_TICKS)
            .text("The script retracts the piston and hides the moving section explicitly before replay restores the scene.")
            .pointAt(util.vector().centerOf(movable))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(STEP_TICKS);
        scene.markAsFinished();
    }

    static void redstoneLampPower(SceneBuilder scene, SceneBuildingUtil util) {
        setup(scene, "redstone_lamp_power", "Powering a Redstone Lamp", util, util.grid().at(1, 1, 2));
        BlockPos source = util.grid().at(1, 1, 2);
        BlockPos firstWire = util.grid().at(2, 1, 2);
        BlockPos secondWire = util.grid().at(3, 1, 2);
        BlockPos lamp = util.grid().at(4, 1, 2);
        Selection wire = util.select().fromTo(firstWire, secondWire);

        scene.overlay().showText(TEXT_TICKS)
            .text("A redstone block supplies power to the wire leading toward the lamp.")
            .pointAt(util.vector().centerOf(source))
            .placeNearTarget()
            .attachKeyFrame();
        scene.overlay().showOutline(PonderPalette.RED, "redstone_source",
            util.select().position(source), TEXT_TICKS);
        scene.idle(STEP_TICKS);

        scene.world().modifyBlocks(wire,
            state -> state.withProperty(BlockRedstoneWire.POWER, Integer.valueOf(15)), true);
        scene.effects().indicateRedstone(firstWire);
        scene.effects().indicateRedstone(secondWire);
        scene.overlay().showText(TEXT_TICKS)
            .text("The scene sets both wire segments to power level 15 and marks the signal path with particles.")
            .pointAt(wire.getCenter())
            .placeNearTarget()
            .attachKeyFrame();
        scene.overlay().showOutline(PonderPalette.RED, "redstone_wire", wire, TEXT_TICKS);
        scene.idle(STEP_TICKS);

        scene.world().setBlock(lamp, Blocks.LIT_REDSTONE_LAMP.getDefaultState(), true);
        scene.effects().createRedstoneParticles(lamp, 0xff3030, 18);
        scene.overlay().showText(TEXT_TICKS)
            .text("The lamp is then replaced with its lit state; no neighbor simulation is required for the demonstration.")
            .pointAt(util.vector().centerOf(lamp))
            .placeNearTarget()
            .attachKeyFrame();
        scene.effects().indicateSuccess(lamp);
        scene.idle(STEP_TICKS);

        scene.world().modifyBlocks(wire,
            state -> state.withProperty(BlockRedstoneWire.POWER, Integer.valueOf(0)), true);
        scene.world().setBlock(lamp, Blocks.REDSTONE_LAMP.getDefaultState(), true);
        scene.overlay().showText(TEXT_TICKS)
            .text("Finally, the wire and lamp return to their unpowered states so the whole transition is visible.")
            .pointAt(util.vector().centerOf(lamp))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(STEP_TICKS);
        scene.markAsFinished();
    }

    static void glassRenderLayers(SceneBuilder scene, SceneBuildingUtil util) {
        setup(scene, "glass_render_layers", "Understanding Render Layers", util, util.grid().at(1, 1, 2));
        BlockPos solid = util.grid().at(1, 1, 2);
        BlockPos cutout = util.grid().at(2, 1, 2);
        BlockPos cutoutMipped = util.grid().at(3, 1, 2);
        BlockPos translucent = util.grid().at(4, 1, 2);

        renderLayerStep(scene, util, solid, PonderPalette.WHITE, "render_solid",
            "Solid blocks write every visible pixel in the solid render layer.");
        renderLayerStep(scene, util, cutout, PonderPalette.INPUT, "render_cutout",
            "Cutout blocks discard transparent pixels to produce sharp-edged details.");
        renderLayerStep(scene, util, cutoutMipped, PonderPalette.GREEN, "render_cutout_mipped",
            "Mipmap cutout blocks use filtered textures while keeping transparent gaps.");
        renderLayerStep(scene, util, translucent, PonderPalette.BLUE, "render_translucent",
            "Stained glass uses the translucent layer, blending its surface with geometry behind it.");
        scene.markAsFinished();
    }

    static void waterHandling(SceneBuilder scene, SceneBuildingUtil util) {
        setup(scene, "water_handling", "Handling Water in a Scene", util, util.grid().at(2, 1, 2));
        BlockPos source = util.grid().at(2, 1, 2);
        BlockPos leftFlow = util.grid().at(1, 1, 2);
        BlockPos rightFlow = util.grid().at(3, 1, 2);
        Selection fluids = util.select().position(source)
            .add(util.select().position(leftFlow))
            .add(util.select().position(rightFlow));

        scene.overlay().showText(TEXT_TICKS)
            .text("The center block is a full water source, while the side blocks show lower flowing levels.")
            .pointAt(util.vector().centerOf(source))
            .placeNearTarget()
            .attachKeyFrame();
        scene.overlay().showOutline(PonderPalette.BLUE, "fluid_source",
            util.select().position(source), TEXT_TICKS);
        scene.idle(STEP_TICKS);

        scene.overlay().showControls(util.vector().topOf(source), Pointing.DOWN, 90)
            .rightClick().withItem(new ItemStack(Items.BUCKET));
        scene.world().setBlocks(fluids, Blocks.AIR.getDefaultState(), true);
        scene.effects().emitParticles(util.vector().topOf(source),
            scene.effects().particleEmitterWithinBlockSpace(EnumParticleTypes.WATER_SPLASH, new Vec3d(0, .03, 0)),
            1, 40);
        scene.overlay().showText(TEXT_TICKS)
            .text("Picking up the source is represented by explicitly clearing the source and both flow blocks.")
            .pointAt(util.vector().centerOf(source))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(STEP_TICKS);

        scene.overlay().showControls(util.vector().topOf(source), Pointing.DOWN, 90)
            .rightClick().withItem(new ItemStack(Items.WATER_BUCKET));
        scene.world().setBlock(source, Blocks.WATER.getDefaultState(), true);
        scene.world().setBlock(leftFlow, Blocks.FLOWING_WATER.getDefaultState()
            .withProperty(BlockLiquid.LEVEL, Integer.valueOf(3)), true);
        scene.world().setBlock(rightFlow, Blocks.FLOWING_WATER.getDefaultState()
            .withProperty(BlockLiquid.LEVEL, Integer.valueOf(5)), true);
        scene.overlay().showText(TEXT_TICKS)
            .text("Placing the bucket restores the source and two explicitly chosen flow levels.")
            .pointAt(util.vector().centerOf(source))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(STEP_TICKS);

        Selection retainingGlass = util.select().fromTo(1, 1, 1, 3, 1, 1)
            .add(util.select().fromTo(1, 1, 3, 3, 1, 3));
        scene.overlay().showOutline(PonderPalette.OUTPUT, "fluid_glass", retainingGlass, TEXT_TICKS);
        scene.overlay().showText(TEXT_TICKS)
            .text("Transparent glass edges keep the water surface visible while showing the blocks behind it.")
            .pointAt(util.vector().topOf(source))
            .placeNearTarget()
            .attachKeyFrame();
        scene.effects().indicateSuccess(source);
        scene.idle(STEP_TICKS);
        scene.markAsFinished();
    }

    static void railMinecart(SceneBuilder scene, SceneBuildingUtil util) {
        setup(scene, "rail_minecart", "Moving a Minecart along Rails", util, util.grid().at(1, 1, 1));
        BlockPos start = util.grid().at(1, 1, 1);
        BlockPos curve = util.grid().at(1, 1, 3);
        BlockPos end = util.grid().at(3, 1, 3);
        Selection track = util.select().fromTo(1, 1, 1, 1, 1, 3)
            .add(util.select().fromTo(2, 1, 3, 3, 1, 3));

        scene.overlay().showText(TEXT_TICKS)
            .text("This rail path travels south, turns at the curve, and then continues east.")
            .pointAt(track.getCenter())
            .placeNearTarget()
            .attachKeyFrame();
        scene.overlay().showOutline(PonderPalette.INPUT, "rail_path", track, TEXT_TICKS);
        scene.idle(STEP_TICKS);

        ElementLink<MinecartElement> cart = scene.special().createCart(
            new Vec3d(1.5, 1.1, 1.5), 0, EntityMinecartEmpty::new);
        scene.special().moveCart(cart, new Vec3d(0, 0, 2), 80);
        scene.overlay().showText(TEXT_TICKS)
            .text("A virtual minecart is created at the start and animated toward the corner.")
            .pointAt(util.vector().centerOf(curve))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(STEP_TICKS);

        scene.special().rotateCart(cart, -90, 55);
        scene.special().moveCart(cart, new Vec3d(2, 0, 0), 85);
        scene.overlay().showText(TEXT_TICKS)
            .text("At the curve, the cart rotates and moves east along the final straight section.")
            .pointAt(util.vector().centerOf(end))
            .placeNearTarget()
            .attachKeyFrame();
        scene.rotateCameraY(-25);
        scene.idle(STEP_TICKS);

        scene.special().hideElement(cart, EnumFacing.UP);
        scene.effects().indicateSuccess(end);
        scene.overlay().showText(TEXT_TICKS)
            .text("Animated elements can be hidden cleanly, and replay creates the cart at its start again.")
            .pointAt(util.vector().topOf(end))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(STEP_TICKS);
        scene.markAsFinished();
    }

    private static void setup(SceneBuilder scene, String id, String title, SceneBuildingUtil util,
                              BlockPos revealOrigin) {
        scene.title(id, title);
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(20);
        scene.addInstruction(new StaggeredDisplayWorldSectionInstruction(
            util.select().layersFrom(1), scene.getScene()::getBaseWorldSection, revealOrigin));
        scene.idle(20);
    }

    private static void renderLayerStep(SceneBuilder scene, SceneBuildingUtil util, BlockPos position,
                                        PonderPalette color, String outlineSlot, String text) {
        scene.overlay().showOutline(color, outlineSlot, util.select().position(position), TEXT_TICKS);
        scene.overlay().showText(TEXT_TICKS)
            .text(text)
            .pointAt(util.vector().centerOf(position))
            .placeNearTarget()
            .attachKeyFrame();
        scene.rotateCameraY(12);
        scene.idle(STEP_TICKS);
    }

    private static ResourceLocation component(String path) {
        return new ResourceLocation("minecraft", path);
    }
}
