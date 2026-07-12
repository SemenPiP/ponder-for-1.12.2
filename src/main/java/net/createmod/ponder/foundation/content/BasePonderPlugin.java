package net.createmod.ponder.foundation.content;

import net.createmod.ponder.Ponder;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.api.registration.SharedTextRegistrationHelper;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;

/** Vanilla-only reference plugin proving the public registration and scene DSL end to end. */
public final class BasePonderPlugin implements PonderPlugin {
    public static final ResourceLocation BASICS = id("basics");
    static final ResourceLocation STORAGE = id("storage");
    static final ResourceLocation MECHANICS = id("mechanics");
    static final ResourceLocation REDSTONE = id("redstone");
    static final ResourceLocation RENDERING = id("rendering");

    @Override public String getModId() { return Ponder.MOD_ID; }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        VanillaPonderScenes.registerAll(helper);
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.registerTag(BASICS)
            .title("Ponder Basics")
            .description("Learn the scene controls and the building blocks used by Ponder tutorials.")
            .item(Blocks.CRAFTING_TABLE)
            .addToIndex()
            .register();
        helper.registerTag(STORAGE)
            .title("Storage")
            .description("Inspect inventories, item entities and block entity data.")
            .item(Blocks.CHEST)
            .addToIndex()
            .register();
        helper.registerTag(MECHANICS)
            .title("Mechanics")
            .description("Follow explicit movement, processing and transport sequences.")
            .item(Blocks.PISTON)
            .addToIndex()
            .register();
        helper.registerTag(REDSTONE)
            .title("Redstone")
            .description("See powered states and signals change step by step.")
            .item(Blocks.REDSTONE_LAMP)
            .addToIndex()
            .register();
        helper.registerTag(RENDERING)
            .title("Rendering")
            .description("Compare render layers and transparent fluids in the virtual world.")
            .item(Blocks.GLASS)
            .addToIndex()
            .register();

        helper.addToTag(BASICS).add(component("crafting_table"));
        helper.addToTag(STORAGE).add(component("chest"));
        helper.addToTag(MECHANICS)
            .add(component("furnace"))
            .add(component("piston"))
            .add(component("rail"));
        helper.addToTag(REDSTONE).add(component("redstone_lamp"));
        helper.addToTag(RENDERING)
            .add(component("glass"))
            .add(component("water_bucket"));
    }

    @Override
    public void registerSharedText(SharedTextRegistrationHelper helper) {
        helper.registerSharedText("demo.controls", "Drag to rotate and scroll to zoom");
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(Ponder.MOD_ID, path);
    }

    private static ResourceLocation component(String path) {
        return new ResourceLocation("minecraft", path);
    }
}
