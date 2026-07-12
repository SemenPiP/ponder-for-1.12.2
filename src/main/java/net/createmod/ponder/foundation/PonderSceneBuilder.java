package net.createmod.ponder.foundation;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.ParticleEmitter;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.AnimatedSceneElement;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.element.InputElementBuilder;
import net.createmod.ponder.api.element.MinecartElement;
import net.createmod.ponder.api.element.ParrotElement;
import net.createmod.ponder.api.element.ParrotPose;
import net.createmod.ponder.api.element.PonderElement;
import net.createmod.ponder.api.element.TextElementBuilder;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.DebugInstructions;
import net.createmod.ponder.api.scene.EffectInstructions;
import net.createmod.ponder.api.scene.OverlayInstructions;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.api.scene.SpecialInstructions;
import net.createmod.ponder.api.scene.WorldInstructions;
import net.createmod.ponder.foundation.element.ElementLinkImpl;
import net.createmod.ponder.foundation.element.OverlayDataElement;
import net.createmod.ponder.foundation.instruction.AnimateMinecartInstruction;
import net.createmod.ponder.foundation.instruction.AnimateParrotInstruction;
import net.createmod.ponder.foundation.instruction.AnimateWorldSectionInstruction;
import net.createmod.ponder.foundation.instruction.BlockEntityDataInstruction;
import net.createmod.ponder.foundation.instruction.DisplayWorldSectionInstruction;
import net.createmod.ponder.foundation.instruction.EmitParticlesInstruction;
import net.createmod.ponder.foundation.instruction.FadeOutOfSceneInstruction;
import net.createmod.ponder.foundation.instruction.MarkAsFinishedInstruction;
import net.createmod.ponder.foundation.instruction.PonderInstruction;
import net.createmod.ponder.foundation.instruction.ReplaceBlocksInstruction;
import net.createmod.ponder.foundation.instruction.ShowElementInstruction;
import net.createmod.ponder.foundation.instruction.StaggeredDisplayWorldSectionInstruction;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class PonderSceneBuilder implements SceneBuilder {
    private final PonderScene scene;
    private final OverlayInstructions overlay = new Overlay();
    private final WorldInstructions world = new WorldOps();
    private final EffectInstructions effects = new Effects();
    private final SpecialInstructions special = new Special();
    private final DebugInstructions debug = new Debug();

    public PonderSceneBuilder(PonderScene scene) {
        if (scene == null) throw new IllegalArgumentException("Scene is required");
        this.scene = scene;
    }

    @Override public OverlayInstructions overlay() { return overlay; }
    @Override public WorldInstructions world() { return world; }
    @Override public DebugInstructions debug() { return debug; }
    @Override public EffectInstructions effects() { return effects; }
    @Override public SpecialInstructions special() { return special; }
    @Override public PonderScene getScene() { return scene; }
    @Override public void title(String sceneId, String title) { scene.setSceneTitle(sceneId, title); }
    @Override public void configureBasePlate(int xOffset, int zOffset, int size) { scene.configureBasePlate(xOffset, zOffset, size); }
    @Override public void scaleSceneView(float factor) { scene.setScale(factor); }
    @Override public void removeShadow() { scene.setShadowShown(false); }
    @Override public void setSceneOffsetY(float yOffset) { scene.setYOffset(yOffset); }

    @Override public void showBasePlate() {
        scene.setBasePlateShown(true);
        showBasePlateSection();
    }

    private void showBasePlateSection() {
        int x = scene.getBasePlateOffsetX();
        int z = scene.getBasePlateOffsetZ();
        int size = scene.getBasePlateSize();
        world.showSection(scene.getSceneBuildingUtil().select().fromTo(x, 0, z, x + size - 1, 0, z + size - 1),
            EnumFacing.UP);
    }

    @Override public void addInstruction(PonderInstruction instruction) { scene.schedule(instruction); }
    @Override public void addInstruction(Consumer<PonderScene> callback) { addInstruction(PonderInstruction.simple(callback)); }
    @Override public void idle(int ticks) { scene.advanceBuildCursor(ticks); }
    @Override public void idleSeconds(int seconds) { idle(Math.multiplyExact(seconds, 20)); }
    @Override public void markAsFinished() { addInstruction(new MarkAsFinishedInstruction()); }
    @Override public void setNextUpEnabled(boolean enabled) { scene.setNextUpEnabled(enabled); }

    @Override public void rotateCameraY(final float degrees) {
        final float start = scene.getCameraYaw();
        addInstruction(new net.createmod.ponder.foundation.instruction.TickingInstruction(false, 10) {
            @Override protected void tickRunning(PonderScene value, int elapsed, float progress) {
                value.setCameraYaw(start + degrees * progress * progress * (3 - 2 * progress));
            }
        });
    }

    @Override public void addKeyframe() { scene.declareKeyframe(scene.getBuildCursor()); }
    @Override public void addLazyKeyframe() { scene.declareKeyframe(scene.getBuildCursor() + 6); }

    private final class Effects implements EffectInstructions {
        @Override public void emitParticles(Vec3d location, ParticleEmitter emitter, float amount, int cycles) {
            addInstruction(new EmitParticlesInstruction(location, emitter, amount, cycles));
        }

        @Override public ParticleEmitter simpleParticleEmitter(final EnumParticleTypes type, final Vec3d motion,
                                                                final int... arguments) {
            return (world, x, y, z) -> world.spawnParticle(type, x, y, z, motion.x, motion.y, motion.z, arguments);
        }

        @Override public ParticleEmitter particleEmitterWithinBlockSpace(final EnumParticleTypes type,
                                                                          final Vec3d motion, final int... arguments) {
            return (world, x, y, z) -> world.spawnParticle(type, x + world.rand.nextDouble() - .5,
                y + world.rand.nextDouble() - .5, z + world.rand.nextDouble() - .5,
                motion.x, motion.y, motion.z, arguments);
        }

        @Override public void indicateRedstone(BlockPos pos) { createRedstoneParticles(pos, 0xff2020, 10); }
        @Override public void indicateSuccess(BlockPos pos) {
            emitParticles(new Vec3d(pos).add(.5, .7, .5), simpleParticleEmitter(EnumParticleTypes.VILLAGER_HAPPY,
                new Vec3d(0, .05, 0)), 2, 8);
        }

        @Override public void createRedstoneParticles(final BlockPos pos, final int color, final int amount) {
            addInstruction(value -> {
                if (value.getWorld() == null) return;
                for (int i = 0; i < Math.max(0, amount); i++)
                    value.getWorld().spawnParticle(EnumParticleTypes.REDSTONE,
                        pos.getX() + .2 + value.getWorld().rand.nextDouble() * .6,
                        pos.getY() + .2 + value.getWorld().rand.nextDouble() * .6,
                        pos.getZ() + .2 + value.getWorld().rand.nextDouble() * .6, 0, 0, 0);
            });
        }
    }

    private final class Overlay implements OverlayInstructions {
        @Override public TextElementBuilder showText(int duration) {
            TextElementBuilder builder = PonderElementFactories.get().createText(scene, duration);
            addInstruction(new ShowElementInstruction(PonderElementFactories.get().asElement(builder), duration, true));
            return builder;
        }

        @Override public TextElementBuilder showOutlineWithText(Selection selection, int duration) {
            showOutline(PonderPalette.WHITE, selection, selection, duration);
            return showText(duration).pointAt(selection.getCenter()).placeNearTarget();
        }

        @Override public InputElementBuilder showControls(Vec3d location, Pointing direction, int duration) {
            InputElementBuilder builder = PonderElementFactories.get().createInput(scene, location, direction, duration);
            addInstruction(new ShowElementInstruction(PonderElementFactories.get().asElement(builder), duration, true));
            return builder;
        }

        @Override public void chaseBoundingBoxOutline(PonderPalette color, Object slot, AxisAlignedBB box, int duration) {
            showMarker(OverlayDataElement.bounds(color, slot, box), duration);
        }

        @Override public void showCenteredScrollInput(BlockPos pos, EnumFacing side, int duration) {
            showScrollInput(new Vec3d(pos).add(.5, .5, .5), side, duration);
        }

        @Override public void showScrollInput(Vec3d location, EnumFacing side, int duration) {
            showControls(location, pointingFor(side), duration).scroll();
        }

        @Override public void showRepeaterScrollInput(BlockPos pos, int duration) {
            showCenteredScrollInput(pos, EnumFacing.UP, duration);
        }

        @Override public void showFilterSlotInput(Vec3d location, int duration) {
            showControls(location, Pointing.DOWN, duration).rightClick();
        }

        @Override public void showFilterSlotInput(Vec3d location, EnumFacing side, int duration) {
            showControls(location, pointingFor(side), duration).rightClick();
        }

        @Override public void showLine(PonderPalette color, Vec3d start, Vec3d end, int duration) {
            showMarker(OverlayDataElement.line(color, start, end, false), duration);
        }

        @Override public void showBigLine(PonderPalette color, Vec3d start, Vec3d end, int duration) {
            showMarker(OverlayDataElement.line(color, start, end, true), duration);
        }

        @Override public void showOutline(PonderPalette color, Object slot, Selection selection, int duration) {
            showMarker(OverlayDataElement.outline(color, slot, selection), duration);
        }

        private void showMarker(OverlayDataElement marker, int duration) {
            addInstruction(new ShowElementInstruction(marker, duration, true));
        }

        private Pointing pointingFor(EnumFacing face) {
            if (face == EnumFacing.WEST) return Pointing.LEFT;
            if (face == EnumFacing.EAST) return Pointing.RIGHT;
            if (face == EnumFacing.DOWN || face == EnumFacing.NORTH) return Pointing.UP;
            return Pointing.DOWN;
        }
    }

    private final class Special implements SpecialInstructions {
        @Override public ElementLink<ParrotElement> createBirb(Vec3d location, Supplier<? extends ParrotPose> pose) {
            final ParrotElement element = PonderElementFactories.get().createParrot(location, pose);
            final ElementLinkImpl<ParrotElement> link = new ElementLinkImpl<ParrotElement>(ParrotElement.class);
            addInstruction(value -> { value.addElement(element); value.linkElement(element, link); element.setVisible(true); });
            return link;
        }

        @Override public void changeBirbPose(ElementLink<ParrotElement> birb, Supplier<? extends ParrotPose> pose) {
            addInstruction(value -> value.runWith(birb, element -> element.setPose(pose.get())));
        }

        @Override public void movePointOfInterest(Vec3d location) { addInstruction(value -> value.setPointOfInterest(location)); }
        @Override public void movePointOfInterest(BlockPos location) { movePointOfInterest(new Vec3d(location).add(.5, .5, .5)); }
        @Override public void rotateParrot(ElementLink<ParrotElement> link, double x, double y, double z, int duration) {
            addInstruction(AnimateParrotInstruction.rotate(link, new Vec3d(x, y, z), duration));
        }
        @Override public void moveParrot(ElementLink<ParrotElement> link, Vec3d offset, int duration) {
            addInstruction(AnimateParrotInstruction.move(link, offset, duration));
        }

        @Override public ElementLink<MinecartElement> createCart(Vec3d location, float angle,
                                                                 MinecartElement.MinecartConstructor constructor) {
            final MinecartElement element = PonderElementFactories.get().createMinecart(location, angle, constructor);
            final ElementLinkImpl<MinecartElement> link = new ElementLinkImpl<MinecartElement>(MinecartElement.class);
            addInstruction(value -> { value.addElement(element); value.linkElement(element, link); element.setVisible(true); });
            return link;
        }

        @Override public void rotateCart(ElementLink<MinecartElement> link, float yaw, int duration) {
            addInstruction(AnimateMinecartInstruction.rotate(link, yaw, duration));
        }
        @Override public void moveCart(ElementLink<MinecartElement> link, Vec3d offset, int duration) {
            addInstruction(AnimateMinecartInstruction.move(link, offset, duration));
        }
        @Override public <T extends AnimatedSceneElement> void hideElement(ElementLink<T> link, EnumFacing direction) {
            addInstruction(new FadeOutOfSceneInstruction<T>(15, direction, link));
        }
    }

    private final class WorldOps implements WorldInstructions {
        @Override public void incrementBlockBreakingProgress(final BlockPos pos) {
            addInstruction(value -> {
                if (value.getWorld() == null) return;
                int id = pos.hashCode();
                PonderWorld.BreakProgress existing = value.getWorld().getBlockBreakingProgress().get(id);
                int next = existing == null ? 0 : existing.progress + 1;
                value.getWorld().setBlockBreakingProgress(id, pos, next > 9 ? -1 : next);
            });
        }

        @Override public void showSection(Selection selection, EnumFacing direction) {
            addInstruction(new StaggeredDisplayWorldSectionInstruction(selection, scene::getBaseWorldSection));
        }
        @Override public void showSectionAndMerge(Selection selection, EnumFacing direction,
                                                  ElementLink<WorldSectionElement> link) {
            addInstruction(new DisplayWorldSectionInstruction(15, direction, selection, () -> scene.resolve(link)));
        }
        @Override public void glueBlockOnto(BlockPos pos, EnumFacing direction, ElementLink<WorldSectionElement> link) {
            addInstruction(new DisplayWorldSectionInstruction(15, direction, scene.getSceneBuildingUtil().select().position(pos),
                () -> scene.resolve(link), pos));
        }
        @Override public ElementLink<WorldSectionElement> showIndependentSection(Selection selection, EnumFacing direction) {
            DisplayWorldSectionInstruction instruction = new DisplayWorldSectionInstruction(15, direction, selection, null);
            addInstruction(instruction); return instruction.createLink(scene);
        }
        @Override public ElementLink<WorldSectionElement> showIndependentSectionImmediately(Selection selection) {
            DisplayWorldSectionInstruction instruction = new DisplayWorldSectionInstruction(0, EnumFacing.DOWN, selection, null);
            addInstruction(instruction); return instruction.createLink(scene);
        }
        @Override public void hideSection(final Selection selection, EnumFacing direction) {
            final WorldSectionElement element = PonderElementFactories.get().createWorldSection(selection);
            final ElementLinkImpl<WorldSectionElement> link = new ElementLinkImpl<WorldSectionElement>(WorldSectionElement.class);
            addInstruction(value -> {
                value.getBaseWorldSection().erase(selection);
                value.addElement(element); value.linkElement(element, link); element.setVisible(true); element.forceApplyFade(1);
            });
            hideIndependentSection(link, direction);
        }
        @Override public void hideIndependentSection(ElementLink<WorldSectionElement> link, EnumFacing direction) {
            addInstruction(new FadeOutOfSceneInstruction<WorldSectionElement>(15, direction, link));
        }
        @Override public void restoreBlocks(Selection selection) {
            addInstruction(value -> { if (value.getWorld() != null) value.getWorld().restoreBlocks(selection); });
        }
        @Override public ElementLink<WorldSectionElement> makeSectionIndependent(final Selection selection) {
            final WorldSectionElement element = PonderElementFactories.get().createWorldSection(selection);
            final ElementLinkImpl<WorldSectionElement> link = new ElementLinkImpl<WorldSectionElement>(WorldSectionElement.class);
            addInstruction(value -> {
                value.getBaseWorldSection().erase(selection); value.addElement(element); value.linkElement(element, link);
                element.setAnimatedOffset(Vec3d.ZERO, true); element.setAnimatedRotation(Vec3d.ZERO, true);
                element.forceApplyFade(1); element.setVisible(true); element.queueRedraw();
            });
            return link;
        }
        @Override public void rotateSection(ElementLink<WorldSectionElement> link, double x, double y, double z, int duration) {
            addInstruction(AnimateWorldSectionInstruction.rotate(link, new Vec3d(x, y, z), duration));
        }
        @Override public void configureCenterOfRotation(ElementLink<WorldSectionElement> link, Vec3d anchor) {
            addInstruction(value -> value.runWith(link, element -> element.setCenterOfRotation(anchor)));
        }
        @Override public void configureStabilization(ElementLink<WorldSectionElement> link, Vec3d anchor) {
            addInstruction(value -> value.runWith(link, element -> element.stabilizeRotation(anchor)));
        }
        @Override public void moveSection(ElementLink<WorldSectionElement> link, Vec3d offset, int duration) {
            addInstruction(AnimateWorldSectionInstruction.move(link, offset, duration));
        }
        @Override public void setBlocks(Selection selection, IBlockState state, boolean particles) {
            addInstruction(new ReplaceBlocksInstruction(selection, ignored -> state, true, particles));
        }
        @Override public void destroyBlock(BlockPos pos) { setBlock(pos, Blocks.AIR.getDefaultState(), true); }
        @Override public void setBlock(BlockPos pos, IBlockState state, boolean particles) {
            setBlocks(scene.getSceneBuildingUtil().select().position(pos), state, particles);
        }
        @Override public void replaceBlocks(Selection selection, IBlockState state, boolean particles) {
            modifyBlocks(selection, ignored -> state, particles);
        }
        @Override public void modifyBlock(BlockPos pos, UnaryOperator<IBlockState> function, boolean particles) {
            modifyBlocks(scene.getSceneBuildingUtil().select().position(pos), function, particles);
        }
        @SuppressWarnings({"rawtypes", "unchecked"})
        @Override public void cycleBlockProperty(BlockPos pos, final IProperty<?> property) {
            modifyBlock(pos, state -> state.getPropertyKeys().contains(property) ? state.cycleProperty((IProperty) property) : state, false);
        }
        @Override public void modifyBlocks(Selection selection, UnaryOperator<IBlockState> function, boolean particles) {
            addInstruction(new ReplaceBlocksInstruction(selection, function, false, particles));
        }
        @SuppressWarnings({"rawtypes", "unchecked"})
        @Override public void toggleRedstonePower(Selection selection) {
            modifyBlocks(selection, state -> {
                for (IProperty property : state.getPropertyKeys()) {
                    String name = property.getName();
                    Comparable value = state.getValue(property);
                    if (("powered".equals(name) || "lit".equals(name)) && value instanceof Boolean)
                        state = state.withProperty(property, !((Boolean) value));
                    else if ("power".equals(name) && value instanceof Integer)
                        state = state.withProperty(property, ((Integer) value) == 0 ? 15 : 0);
                }
                return state;
            }, false);
        }
        @Override public <T extends Entity> void modifyEntities(Class<T> type, Consumer<T> callback) {
            addInstruction(value -> value.forEachWorldEntity(type, callback));
        }
        @Override public <T extends Entity> void modifyEntitiesInside(Class<T> type, Selection area, Consumer<T> callback) {
            addInstruction(value -> value.forEachWorldEntity(type, entity -> { if (area.test(entity.getPosition())) callback.accept(entity); }));
        }
        @Override public void modifyEntity(ElementLink<EntityElement> link, Consumer<Entity> callback) {
            addInstruction(value -> value.runWith(link, element -> element.ifPresent(callback)));
        }
        @Override public ElementLink<EntityElement> createEntity(final Function<World, Entity> factory) {
            final ElementLinkImpl<EntityElement> link = new ElementLinkImpl<EntityElement>(EntityElement.class, UUID.randomUUID());
            addInstruction(value -> {
                if (value.getWorld() == null) return;
                Entity entity = factory.apply(value.getWorld());
                if (entity == null) return;
                value.getWorld().spawnEntity(entity);
                EntityElement element = PonderElementFactories.get().createEntity(entity);
                value.addElement(element); value.linkElement(element, link); element.setVisible(true);
            });
            return link;
        }
        @Override public ElementLink<EntityElement> createItemEntity(final Vec3d location, final Vec3d motion,
                                                                      final ItemStack stack) {
            return createEntity(world -> {
                EntityItem item = new EntityItem(world, location.x, location.y, location.z, stack.copy());
                item.setPosition(location.x, location.y, location.z);
                item.prevPosX = item.lastTickPosX = location.x;
                item.prevPosY = item.lastTickPosY = location.y;
                item.prevPosZ = item.lastTickPosZ = location.z;
                item.motionX = motion.x;
                item.motionY = motion.y;
                item.motionZ = motion.z;
                item.hoverStart = 0;
                return item;
            });
        }
        @Override public void modifyBlockEntityNBT(Selection selection, Class<? extends TileEntity> type,
                                                    Consumer<NBTTagCompound> consumer) {
            modifyBlockEntityNBT(selection, type, consumer, false);
        }
        @Override public <T extends TileEntity> void modifyBlockEntity(final BlockPos pos, final Class<T> type,
                                                                       final Consumer<T> consumer) {
            addInstruction(value -> {
                if (value.getWorld() == null) return;
                TileEntity tile = value.getWorld().getTileEntity(pos);
                if (type.isInstance(tile)) consumer.accept(type.cast(tile));
            });
        }
        @Override public void modifyBlockEntityNBT(Selection selection, Class<? extends TileEntity> type,
                                                    Consumer<NBTTagCompound> consumer, boolean redraw) {
            addInstruction(new BlockEntityDataInstruction(selection, type, consumer, redraw));
        }
    }

    private final class Debug implements DebugInstructions {
        @Override public void debugSchematic() { world.showSection(scene.getSceneBuildingUtil().select().everywhere(), EnumFacing.UP); }
        @Override public void addInstructionInstance(PonderInstruction instruction) { addInstruction(instruction); }
        @Override public void enqueueCallback(Consumer<PonderScene> callback) { addInstruction(callback); }
    }
}
