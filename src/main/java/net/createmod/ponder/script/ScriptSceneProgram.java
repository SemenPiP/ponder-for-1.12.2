package net.createmod.ponder.script;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.script.ScriptInstructionCodec;
import net.createmod.ponder.api.script.ScriptInstructionCodecs;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.element.MinecartElement;
import net.createmod.ponder.api.element.ParrotElement;
import net.createmod.ponder.api.element.ParrotPose;
import net.createmod.ponder.api.element.InputElementBuilder;
import net.createmod.ponder.api.element.TextElementBuilder;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.instruction.AnimateEntityInstruction;
import net.createmod.ponder.foundation.instruction.EntityElementInstruction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

final class ScriptSceneProgram {
    private ScriptSceneProgram() {
    }

    static void program(ScriptSceneDefinition definition, SceneBuilder scene, SceneBuildingUtil util) {
        scene.title(definition.getSceneId().toString(), definition.getTitle());
        Map<String, ElementLink<WorldSectionElement>> sections =
            new HashMap<String, ElementLink<WorldSectionElement>>();
        Map<String, ElementLink<MinecartElement>> minecarts =
            new HashMap<String, ElementLink<MinecartElement>>();
        Map<String, ElementLink<ParrotElement>> parrots =
            new HashMap<String, ElementLink<ParrotElement>>();
        Map<String, ElementLink<EntityElement>> items =
            new HashMap<String, ElementLink<EntityElement>>();
        int index = 0;
        for (ScriptInstruction instruction : definition.getInstructions()) {
            try {
                apply(instruction, scene, util, sections, minecarts, parrots, items);
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("Ponder script " + definition.getSceneId()
                    + " instruction #" + index + " (" + instruction.getOperation() + ") failed: "
                    + exception.getMessage(), exception);
            }
            index++;
        }
    }

    private static void apply(ScriptInstruction instruction, SceneBuilder scene, SceneBuildingUtil util,
                              Map<String, ElementLink<WorldSectionElement>> sections,
                              Map<String, ElementLink<MinecartElement>> minecarts,
                              Map<String, ElementLink<ParrotElement>> parrots,
                              Map<String, ElementLink<EntityElement>> items) {
        String op = instruction.getOperation();
        NBTTagCompound data = instruction.getData();
        if ("configure_base_plate".equals(op)) {
            scene.configureBasePlate(data.getInteger("x"), data.getInteger("z"), data.getInteger("size"));
        } else if ("show_base_plate".equals(op)) {
            scene.showBasePlate();
        } else if ("remove_shadow".equals(op)) {
            scene.removeShadow();
        } else if ("scale".equals(op)) {
            scene.scaleSceneView(data.getFloat("value"));
        } else if ("offset_y".equals(op)) {
            scene.setSceneOffsetY(data.getFloat("value"));
        } else if ("idle".equals(op)) {
            scene.idle(data.getInteger("ticks"));
        } else if ("rotate_camera".equals(op)) {
            scene.rotateCameraY(data.getFloat("degrees"));
        } else if ("keyframe".equals(op)) {
            scene.addKeyframe();
        } else if ("lazy_keyframe".equals(op)) {
            scene.addLazyKeyframe();
        } else if ("finish".equals(op)) {
            scene.markAsFinished();
        } else if ("next_up".equals(op)) {
            scene.setNextUpEnabled(data.getBoolean("enabled"));
        } else if ("show_section".equals(op)) {
            scene.world().showSection(selection(data, util), direction(data.getString("direction")));
        } else if ("hide_section".equals(op)) {
            scene.world().hideSection(selection(data, util), direction(data.getString("direction")));
        } else if ("restore_blocks".equals(op)) {
            scene.world().restoreBlocks(selection(data, util));
        } else if ("show_independent".equals(op)) {
            sections.put(data.getString("handle"), scene.world().showIndependentSection(
                selection(data, util), direction(data.getString("direction"))));
        } else if ("show_independent_immediate".equals(op)) {
            sections.put(data.getString("handle"), scene.world().showIndependentSectionImmediately(selection(data, util)));
        } else if ("make_independent".equals(op)) {
            sections.put(data.getString("handle"), scene.world().makeSectionIndependent(selection(data, util)));
        } else if ("show_section_merge".equals(op)) {
            scene.world().showSectionAndMerge(selection(data, util), direction(data.getString("direction")),
                section(sections, data));
        } else if ("glue_block".equals(op)) {
            scene.world().glueBlockOnto(position(data), direction(data.getString("direction")), section(sections, data));
        } else if ("hide_independent".equals(op)) {
            scene.world().hideIndependentSection(section(sections, data), direction(data.getString("direction")));
        } else if ("move_section".equals(op)) {
            scene.world().moveSection(section(sections, data), vector(data), data.getInteger("duration"));
        } else if ("rotate_section".equals(op)) {
            scene.world().rotateSection(section(sections, data), data.getDouble("x"), data.getDouble("y"),
                data.getDouble("z"), data.getInteger("duration"));
        } else if ("center_section".equals(op)) {
            scene.world().configureCenterOfRotation(section(sections, data), vector(data));
        } else if ("stabilize_section".equals(op)) {
            scene.world().configureStabilization(section(sections, data), vector(data));
        } else if ("set_block".equals(op)) {
            scene.world().setBlock(position(data), ScriptBlockStateParser.parse(data.getString("state")),
                data.getBoolean("particles"));
        } else if ("set_blocks".equals(op)) {
            scene.world().setBlocks(selection(data, util), ScriptBlockStateParser.parse(data.getString("state")),
                data.getBoolean("particles"));
        } else if ("destroy_block".equals(op)) {
            scene.world().destroyBlock(position(data));
        } else if ("break_progress".equals(op)) {
            scene.world().incrementBlockBreakingProgress(position(data));
        } else if ("cycle_property".equals(op)) {
            BlockPos pos = position(data);
            IBlockState state = scene.getScene().getWorld().getBlockState(pos);
            IProperty<?> found = null;
            for (IProperty<?> property : state.getPropertyKeys()) {
                if (property.getName().equals(data.getString("property"))) {
                    found = property;
                    break;
                }
            }
            if (found == null)
                throw new IllegalArgumentException("Block at " + pos + " has no property " + data.getString("property"));
            scene.world().cycleBlockProperty(pos, found);
        } else if ("toggle_redstone".equals(op)) {
            scene.world().toggleRedstonePower(selection(data, util));
        } else if ("create_item".equals(op)) {
            String handle = data.getString("handle");
            requireUnusedHandle(handle, sections, minecarts, parrots, items);
            Item item = Item.REGISTRY.getObject(new ResourceLocation(data.getString("item")));
            if (item == null) throw new IllegalArgumentException("Unknown item " + data.getString("item"));
            items.put(handle, scene.world().createItemEntity(vector(data),
                new Vec3d(data.getDouble("mx"), data.getDouble("my"), data.getDouble("mz")),
                new ItemStack(item, data.getInteger("count"), data.getInteger("meta"))));
        } else if ("move_item".equals(op)) {
            scene.addInstruction(new AnimateEntityInstruction(item(items, data), vector(data),
                data.getInteger("duration")));
        } else if ("set_item_visible".equals(op)) {
            scene.addInstruction(EntityElementInstruction.setVisible(item(items, data), data.getBoolean("visible")));
        } else if ("remove_item".equals(op)) {
            String handle = data.getString("handle");
            scene.addInstruction(EntityElementInstruction.remove(item(items, data)));
            items.remove(handle);
        } else if ("create_minecart".equals(op)) {
            String handle = data.getString("handle");
            requireUnusedHandle(handle, sections, minecarts, parrots, items);
            minecarts.put(handle, scene.special().createCart(vector(data), data.getFloat("angle"),
                minecartConstructor(data.getString("type"))));
        } else if ("move_minecart".equals(op)) {
            scene.special().moveCart(minecart(minecarts, parrots, data), vector(data), data.getInteger("duration"));
        } else if ("rotate_minecart".equals(op)) {
            scene.special().rotateCart(minecart(minecarts, parrots, data), data.getFloat("angle"),
                data.getInteger("duration"));
        } else if ("hide_minecart".equals(op)) {
            scene.special().hideElement(minecart(minecarts, parrots, data),
                direction(data.getString("direction")));
        } else if ("create_parrot".equals(op)) {
            String handle = data.getString("handle");
            requireUnusedHandle(handle, sections, minecarts, parrots, items);
            parrots.put(handle, scene.special().createBirb(vector(data), parrotPose(data.getString("pose"))));
        } else if ("change_parrot_pose".equals(op)) {
            scene.special().changeBirbPose(parrot(parrots, minecarts, data), parrotPose(data.getString("pose")));
        } else if ("move_parrot".equals(op)) {
            scene.special().moveParrot(parrot(parrots, minecarts, data), vector(data), data.getInteger("duration"));
        } else if ("rotate_parrot".equals(op)) {
            scene.special().rotateParrot(parrot(parrots, minecarts, data), data.getDouble("x"), data.getDouble("y"),
                data.getDouble("z"), data.getInteger("duration"));
        } else if ("hide_parrot".equals(op)) {
            scene.special().hideElement(parrot(parrots, minecarts, data), direction(data.getString("direction")));
        } else if ("tile_nbt".equals(op)) {
            final NBTTagCompound replacement = data.getCompoundTag("nbt").copy();
            final boolean replace = data.getBoolean("replace");
            scene.world().modifyBlockEntityNBT(selection(data, util), TileEntity.class, existing -> {
                if (replace) {
                    for (String key : new java.util.HashSet<String>(existing.getKeySet()))
                        existing.removeTag(key);
                }
                existing.merge(replacement);
            }, data.getBoolean("redraw"));
        } else if ("show_text".equals(op)) {
            TextElementBuilder text = scene.overlay().showText(data.getInteger("duration"))
                .text(data.getString("text")).pointAt(vector(data)).colored(palette(data.getString("color")));
            if (data.getBoolean("near")) text.placeNearTarget();
            if (data.getBoolean("keyframe")) text.attachKeyFrame();
        } else if ("show_shared_text".equals(op)) {
            TextElementBuilder text = scene.overlay().showText(data.getInteger("duration"))
                .sharedText(new ResourceLocation(data.getString("key")), stringParameters(data))
                .pointAt(vector(data)).colored(palette(data.getString("color")));
            if (data.getBoolean("near")) text.placeNearTarget();
            if (data.getBoolean("keyframe")) text.attachKeyFrame();
        } else if ("show_independent_text".equals(op)) {
            TextElementBuilder text = scene.overlay().showText(data.getInteger("duration"))
                .text(data.getString("text")).independent(data.getInteger("y"))
                .colored(palette(data.getString("color")));
            if (data.getBoolean("keyframe")) text.attachKeyFrame();
        } else if ("show_outline_text".equals(op)) {
            TextElementBuilder text = scene.overlay().showOutlineWithText(selection(data, util),
                data.getInteger("duration")).text(data.getString("text"))
                .colored(palette(data.getString("color")));
            if (data.getBoolean("keyframe")) text.attachKeyFrame();
        } else if ("show_controls".equals(op)) {
            InputElementBuilder input = scene.overlay().showControls(vector(data), pointing(data.getString("pointing")),
                data.getInteger("duration"));
            String action = data.getString("action");
            if ("left_click".equals(action)) input.leftClick();
            else if ("scroll".equals(action)) input.scroll();
            else input.rightClick();
            if (data.hasKey("item", 8)) {
                Item item = Item.REGISTRY.getObject(new ResourceLocation(data.getString("item")));
                if (item != null) input.withItem(new ItemStack(item));
            }
        } else if ("show_line".equals(op)) {
            Vec3d from = new Vec3d(data.getDouble("x1"), data.getDouble("y1"), data.getDouble("z1"));
            Vec3d to = new Vec3d(data.getDouble("x2"), data.getDouble("y2"), data.getDouble("z2"));
            if (data.getBoolean("big")) scene.overlay().showBigLine(palette(data.getString("color")), from, to,
                data.getInteger("duration"));
            else scene.overlay().showLine(palette(data.getString("color")), from, to, data.getInteger("duration"));
        } else if ("show_outline".equals(op)) {
            scene.overlay().showOutline(palette(data.getString("color")), data.getString("slot"),
                selection(data, util), data.getInteger("duration"));
        } else if ("show_bounding_box".equals(op)) {
            scene.overlay().chaseBoundingBoxOutline(palette(data.getString("color")), data.getString("slot"),
                new AxisAlignedBB(data.getDouble("minX"), data.getDouble("minY"), data.getDouble("minZ"),
                    data.getDouble("maxX"), data.getDouble("maxY"), data.getDouble("maxZ")),
                data.getInteger("duration"));
        } else if ("show_scroll_input".equals(op)) {
            scene.overlay().showScrollInput(vector(data), direction(data.getString("side")),
                data.getInteger("duration"));
        } else if ("show_centered_scroll_input".equals(op)) {
            scene.overlay().showCenteredScrollInput(position(data), direction(data.getString("side")),
                data.getInteger("duration"));
        } else if ("show_repeater_scroll_input".equals(op)) {
            scene.overlay().showRepeaterScrollInput(position(data), data.getInteger("duration"));
        } else if ("show_filter_slot_input".equals(op)) {
            scene.overlay().showFilterSlotInput(vector(data), direction(data.getString("side")),
                data.getInteger("duration"));
        } else if ("indicate_redstone".equals(op)) {
            scene.effects().indicateRedstone(position(data));
        } else if ("indicate_success".equals(op)) {
            scene.effects().indicateSuccess(position(data));
        } else if ("redstone_particles".equals(op)) {
            scene.effects().createRedstoneParticles(position(data), data.getInteger("color"),
                data.getInteger("amount"));
        } else if ("particles".equals(op)) {
            EnumParticleTypes particle = EnumParticleTypes.valueOf(data.getString("type").toUpperCase(Locale.ROOT));
            scene.effects().emitParticles(vector(data), scene.effects().simpleParticleEmitter(particle,
                new Vec3d(data.getDouble("mx"), data.getDouble("my"), data.getDouble("mz"))),
                data.getFloat("amount"), data.getInteger("cycles"));
        } else if ("particles_within_block".equals(op)) {
            EnumParticleTypes particle = EnumParticleTypes.valueOf(data.getString("type").toUpperCase(Locale.ROOT));
            scene.effects().emitParticles(vector(data), scene.effects().particleEmitterWithinBlockSpace(particle,
                new Vec3d(data.getDouble("mx"), data.getDouble("my"), data.getDouble("mz"))),
                data.getFloat("amount"), data.getInteger("cycles"));
        } else if ("move_poi".equals(op)) {
            scene.special().movePointOfInterest(vector(data));
        } else if ("custom".equals(op)) {
            ResourceLocation id = new ResourceLocation(data.getString("codec"));
            ScriptInstructionCodec codec = ScriptInstructionCodecs.get(id);
            if (codec == null) throw new IllegalArgumentException("Missing custom instruction codec " + id);
            codec.validate(data.getCompoundTag("payload").copy());
            codec.program(data.getCompoundTag("payload").copy(), scene, util);
        } else {
            throw new IllegalArgumentException("Unknown Ponder script instruction " + op);
        }
    }

    private static Selection selection(NBTTagCompound data, SceneBuildingUtil util) {
        return ScriptSelection.deserialize(data.getCompoundTag("selection")).resolve(util);
    }

    private static BlockPos position(NBTTagCompound data) {
        return new BlockPos(data.getInteger("x"), data.getInteger("y"), data.getInteger("z"));
    }

    private static Vec3d vector(NBTTagCompound data) {
        return new Vec3d(data.getDouble("x"), data.getDouble("y"), data.getDouble("z"));
    }

    private static EnumFacing direction(String value) {
        return EnumFacing.valueOf(value.toUpperCase(Locale.ROOT));
    }

    private static Pointing pointing(String value) {
        return Pointing.valueOf((value == null || value.isEmpty() ? "down" : value).toUpperCase(Locale.ROOT));
    }

    private static PonderPalette palette(String value) {
        return PonderPalette.valueOf((value == null || value.isEmpty() ? "white" : value).toUpperCase(Locale.ROOT));
    }

    private static ElementLink<WorldSectionElement> section(Map<String, ElementLink<WorldSectionElement>> sections,
                                                             NBTTagCompound data) {
        ElementLink<WorldSectionElement> result = sections.get(data.getString("handle"));
        if (result == null) throw new IllegalArgumentException("Unknown section handle " + data.getString("handle"));
        return result;
    }

    private static ElementLink<MinecartElement> minecart(Map<String, ElementLink<MinecartElement>> minecarts,
                                                          Map<String, ElementLink<ParrotElement>> parrots,
                                                          NBTTagCompound data) {
        ElementLink<MinecartElement> result = minecarts.get(data.getString("handle"));
        if (result == null) {
            if (parrots.containsKey(data.getString("handle")))
                throw new IllegalArgumentException("Handle " + data.getString("handle") + " is a parrot, expected minecart");
            throw new IllegalArgumentException("Unknown minecart handle " + data.getString("handle"));
        }
        return result;
    }

    private static ElementLink<ParrotElement> parrot(Map<String, ElementLink<ParrotElement>> parrots,
                                                       Map<String, ElementLink<MinecartElement>> minecarts,
                                                       NBTTagCompound data) {
        ElementLink<ParrotElement> result = parrots.get(data.getString("handle"));
        if (result == null) {
            if (minecarts.containsKey(data.getString("handle")))
                throw new IllegalArgumentException("Handle " + data.getString("handle") + " is a minecart, expected parrot");
            throw new IllegalArgumentException("Unknown parrot handle " + data.getString("handle"));
        }
        return result;
    }

    private static ElementLink<EntityElement> item(Map<String, ElementLink<EntityElement>> items,
                                                    NBTTagCompound data) {
        ElementLink<EntityElement> result = items.get(data.getString("handle"));
        if (result == null)
            throw new IllegalArgumentException("Unknown or removed item handle " + data.getString("handle"));
        return result;
    }

    private static void requireUnusedHandle(String handle,
                                            Map<String, ElementLink<WorldSectionElement>> sections,
                                            Map<String, ElementLink<MinecartElement>> minecarts,
                                            Map<String, ElementLink<ParrotElement>> parrots,
                                            Map<String, ElementLink<EntityElement>> items) {
        if (sections.containsKey(handle) || minecarts.containsKey(handle)
            || parrots.containsKey(handle) || items.containsKey(handle))
            throw new IllegalArgumentException("Duplicate runtime handle " + handle);
    }

    private static Object[] stringParameters(NBTTagCompound data) {
        NBTTagList values = data.getTagList("params", 8);
        Object[] result = new Object[values.tagCount()];
        for (int i = 0; i < values.tagCount(); i++)
            result[i] = values.getStringTagAt(i);
        return result;
    }

    private static MinecartElement.MinecartConstructor minecartConstructor(String value) {
        String type = ScriptWorldBuilder.minecartType(value);
        if ("chest".equals(type)) return net.minecraft.entity.item.EntityMinecartChest::new;
        if ("furnace".equals(type)) return net.minecraft.entity.item.EntityMinecartFurnace::new;
        if ("hopper".equals(type)) return net.minecraft.entity.item.EntityMinecartHopper::new;
        if ("tnt".equals(type)) return net.minecraft.entity.item.EntityMinecartTNT::new;
        return net.minecraft.entity.item.EntityMinecartEmpty::new;
    }

    private static java.util.function.Supplier<? extends ParrotPose> parrotPose(String value) {
        String pose = ScriptWorldBuilder.parrotPose(value);
        if ("dance".equals(pose)) return ParrotPose.DancePose::new;
        if ("flappy".equals(pose)) return ParrotPose.FlappyPose::new;
        if ("face_cursor".equals(pose)) return ParrotPose.FaceCursorPose::new;
        return ParrotPose.FacePointOfInterestPose::new;
    }
}
