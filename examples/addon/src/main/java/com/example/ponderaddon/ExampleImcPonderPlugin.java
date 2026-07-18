package com.example.ponderaddon;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

/** Second plugin registered through Forge IMC rather than ServiceLoader. */
public final class ExampleImcPonderPlugin implements PonderPlugin {
    @Override public String getModId() { return ExampleAddon.MOD_ID; }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.addStoryBoard(new ResourceLocation("minecraft", "sticky_piston"),
            id("codec_demo"), ExampleImcPonderPlugin::scene)
            .identifiedBy(id("imc_registration"));
    }

    private static void scene(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("imc_registration", "Forge IMC Registration");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.world().showSection(util.select().layersFrom(1), EnumFacing.DOWN);
        scene.idle(40);
        scene.effects().indicateSuccess(util.grid().at(2, 1, 2));
        scene.idle(20);
        scene.markAsFinished();
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(ExampleAddon.MOD_ID, path);
    }
}
