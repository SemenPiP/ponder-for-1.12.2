package net.createmod.ponder.foundation.content;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.ParrotElement;
import net.createmod.ponder.api.element.ParrotPose;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

public final class DebugScenes {
    private DebugScenes() {}

    public static void registerAll(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        ResourceLocation component = new ResourceLocation("minecraft", "spyglass");
        ResourceLocation structure = new ResourceLocation("ponder", "demo/basics");
        helper.addStoryBoard(component, structure, DebugScenes::coordinates);
        helper.addStoryBoard(component, structure, DebugScenes::blocks);
        helper.addStoryBoard(component, structure, DebugScenes::particles);
        helper.addStoryBoard(component, structure, DebugScenes::controls);
        helper.addStoryBoard(component, structure, DebugScenes::sections);
        helper.addStoryBoard(component, structure, DebugScenes::birbs);
    }

    public static void empty(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("debug_empty", "Missing Content"); scene.showBasePlate(); scene.idle(5);
    }

    private static void setup(SceneBuilder scene, SceneBuildingUtil util, String id, String title) {
        scene.title(id, title); scene.configureBasePlate(0, 0, 5); scene.showBasePlate(); scene.idle(5);
        scene.world().showSection(util.select().layersFrom(1), EnumFacing.DOWN); scene.idle(10);
    }

    public static void coordinates(SceneBuilder scene, SceneBuildingUtil util) {
        setup(scene, util, "debug_coordinates", "Coordinate Space");
        scene.overlay().showOutlineWithText(util.select().fromTo(0, 1, 0, 4, 1, 0), 30).colored(PonderPalette.RED).text("X axis");
        scene.overlay().showOutlineWithText(util.select().fromTo(0, 1, 0, 0, 2, 0), 30).colored(PonderPalette.GREEN).text("Y axis");
        scene.overlay().showBigLine(PonderPalette.BLUE, util.vector().centerOf(0, 1, 0), util.vector().centerOf(0, 1, 4), 30);
    }

    public static void blocks(SceneBuilder scene, SceneBuildingUtil util) {
        setup(scene, util, "debug_blocks", "Changing Blocks");
        scene.world().replaceBlocks(util.select().position(2, 1, 2), Blocks.GOLD_BLOCK.getDefaultState(), true);
        for (int i = 0; i < 10; i++) { scene.world().incrementBlockBreakingProgress(util.grid().at(2, 1, 2)); scene.idle(2); }
        scene.world().restoreBlocks(util.select().position(2, 1, 2));
    }

    public static void particles(SceneBuilder scene, SceneBuildingUtil util) {
        setup(scene, util, "debug_particles", "Virtual Particles");
        scene.effects().emitParticles(util.vector().centerOf(2, 1, 2),
            scene.effects().particleEmitterWithinBlockSpace(EnumParticleTypes.FLAME, new Vec3d(0, .03, 0)), 3, 30);
    }

    public static void controls(SceneBuilder scene, SceneBuildingUtil util) {
        setup(scene, util, "debug_controls", "Input Overlays");
        scene.overlay().showControls(util.vector().topOf(2, 1, 2), Pointing.DOWN, 60).rightClick().whileSneaking()
            .withItem(new ItemStack(Items.REDSTONE));
        Vec3d p = util.vector().centerOf(2, 1, 2);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, "debug_box",
            new AxisAlignedBB(p.x - .25, p.y - .25, p.z - .25, p.x + .25, p.y + .25, p.z + .25), 60);
    }

    public static void sections(SceneBuilder scene, SceneBuildingUtil util) {
        setup(scene, util, "debug_sections", "Independent Sections");
        ElementLink<WorldSectionElement> section = scene.world().makeSectionIndependent(util.select().position(3, 1, 2));
        scene.world().configureCenterOfRotation(section, util.vector().centerOf(3, 1, 2));
        scene.world().moveSection(section, new Vec3d(0, 2, 0), 30);
        scene.world().rotateSection(section, 90, 360, 0, 30); scene.idle(35);
        scene.world().hideIndependentSection(section, EnumFacing.UP);
    }

    public static void birbs(SceneBuilder scene, SceneBuildingUtil util) {
        setup(scene, util, "debug_birbs", "Parrot Poses");
        ElementLink<ParrotElement> parrot = scene.special().createBirb(util.vector().topOf(2, 1, 2),
            ParrotPose.FacePointOfInterestPose::new);
        scene.special().movePointOfInterest(util.vector().centerOf(4, 1, 4)); scene.idle(20);
        scene.special().changeBirbPose(parrot, ParrotPose.DancePose::new);
        scene.special().moveParrot(parrot, new Vec3d(0, 1, 0), 20);
    }
}
