package com.example.ponderaddon;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.registration.IndexExclusionHelper;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.api.registration.SharedTextRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

public final class ExamplePonderPlugin implements PonderPlugin {
    private static final ResourceLocation MECHANISMS = id("mechanisms");

    @Override public String getModId() { return ExampleAddon.MOD_ID; }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.addStoryBoard(new ResourceLocation("minecraft", "piston"),
            id("codec_demo"), ExamplePonderPlugin::pistonScene, MECHANISMS)
            .identifiedBy(id("service_loader"))
            .orderBefore("ponder", "ponder_basics");
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.registerTag(MECHANISMS)
            .title("Example Mechanisms")
            .description("Scenes registered by a separate addon through the public Ponder API.")
            .item(Blocks.PISTON)
            .addToIndex()
            .register();
        helper.addToTag(MECHANISMS)
            .add(new ResourceLocation("minecraft", "piston"))
            .add(new ResourceLocation("minecraft", "sticky_piston"));
    }

    @Override
    public void registerSharedText(SharedTextRegistrationHelper helper) {
        helper.registerSharedText("piston.summary", "Sections can move independently from the virtual world");
    }

    @Override
    public void indexExclusions(IndexExclusionHelper helper) {
        helper.exclude(Items.AIR);
    }

    private static void pistonScene(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("piston_sections", "Moving Independent Sections");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);

        Selection machine = util.select().layersFrom(1);
        scene.world().showSection(machine, EnumFacing.DOWN);
        scene.idle(20);
        scene.overlay().showText(65)
            .sharedText("piston.summary")
            .pointAt(util.vector().centerOf(2, 1, 2))
            .placeNearTarget()
            .attachKeyFrame();
        scene.overlay().showControls(util.vector().topOf(2, 1, 2), Pointing.DOWN, 35)
            .rightClick()
            .withItem(new ItemStack(Blocks.PISTON));
        scene.overlay().showOutline(PonderPalette.INPUT, "input", util.select().position(2, 1, 2), 40);
        scene.overlay().showLine(PonderPalette.OUTPUT, util.vector().centerOf(2, 1, 2),
            util.vector().centerOf(3, 1, 2), 40);
        scene.idle(25);

        ElementLink<WorldSectionElement> moving = scene.world().makeSectionIndependent(
            util.select().position(3, 1, 2));
        scene.world().configureCenterOfRotation(moving, util.vector().centerOf(3, 1, 2));
        scene.world().configureStabilization(moving, util.vector().centerOf(3, 1, 2));
        scene.world().moveSection(moving, new Vec3d(0, 1, 0), 20);
        scene.world().rotateSection(moving, 0, 90, 0, 20);
        scene.effects().emitParticles(util.vector().centerOf(3, 1, 2),
            scene.effects().simpleParticleEmitter(EnumParticleTypes.VILLAGER_HAPPY, new Vec3d(0, .04, 0)), 1, 20);
        scene.idle(25);

        scene.world().setBlock(util.grid().at(2, 1, 2), Blocks.PISTON.getDefaultState(), true);
        scene.world().modifyBlock(util.grid().at(2, 1, 2), state -> state, false);
        ElementLink<EntityElement> item = scene.world().createItemEntity(util.vector().topOf(2, 1, 2),
            new Vec3d(0, .1, 0), new ItemStack(Blocks.PISTON));
        scene.world().modifyEntity(item, entity -> entity.setGlowing(true));
        scene.special().movePointOfInterest(util.vector().centerOf(2, 1, 2));
        scene.rotateCameraY(30);
        scene.idle(30);
        scene.world().hideIndependentSection(moving, EnumFacing.UP);
        scene.markAsFinished();
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(ExampleAddon.MOD_ID, path);
    }
}
