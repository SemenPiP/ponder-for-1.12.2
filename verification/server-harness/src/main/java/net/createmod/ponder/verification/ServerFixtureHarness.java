package net.createmod.ponder.verification;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import net.createmod.ponder.api.registration.StoryBoardEntry;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.PonderTag;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;

@Mod(
    modid = ServerFixtureHarness.MOD_ID,
    name = "Ponder Server Fixture Harness",
    version = "1.1.2",
    dependencies = "required-after:ponder_legacy"
)
public final class ServerFixtureHarness {
    public static final String MOD_ID = "ponder_fixture_harness";
    private static final List<Integer> KEYFRAMES = Arrays.asList(40, 190, 340, 490);

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        Properties report = new Properties();
        try {
            verifyBuiltins(report);
            verifyPositiveFixture(report);
            report.setProperty("status", "PASS");
            writeReport(report);
            System.out.println("[Ponder Fixture] PASS: builtin and CraftTweaker scene registration verified");
        } catch (Throwable failure) {
            report.setProperty("status", "FAIL");
            report.setProperty("error", failure.toString());
            try {
                writeReport(report);
            } catch (Exception suppressed) {
                failure.addSuppressed(suppressed);
            }
            throw new IllegalStateException("Ponder server fixture verification failed", failure);
        }
    }

    private static void verifyBuiltins(Properties report) {
        Map<ResourceLocation, ExpectedScene> expected = expectedScenes();
        Map<ResourceLocation, StoryBoardEntry> found = new LinkedHashMap<ResourceLocation, StoryBoardEntry>();
        for (Map.Entry<ResourceLocation, StoryBoardEntry> registered
                : PonderIndex.getSceneAccess().getRegisteredEntries()) {
            ExpectedScene value = expected.get(registered.getKey());
            if (value != null && value.structure.equals(registered.getValue().getSchematicLocation()))
                found.put(registered.getKey(), registered.getValue());
        }
        require(found.size() == expected.size(),
            "Expected 8 builtin scenes, found " + found.size() + " matching entries");

        for (Map.Entry<ResourceLocation, ExpectedScene> expectedEntry : expected.entrySet()) {
            StoryBoardEntry entry = found.get(expectedEntry.getKey());
            ExpectedScene value = expectedEntry.getValue();
            require(entry != null, "Missing builtin component " + expectedEntry.getKey());
            require(value.structure.equals(entry.getSchematicLocation()),
                "Wrong structure for " + expectedEntry.getKey());
            require(entry.getTags().equals(Arrays.asList(value.tag)),
                "Wrong storyboard tag for " + expectedEntry.getKey() + ": " + entry.getTags());
            require(componentTags(expectedEntry.getKey()).contains(value.tag),
                "Component tag mapping is missing for " + expectedEntry.getKey());

            List<PonderScene> compiled = PonderIndex.getSceneAccess()
                .compile(java.util.Collections.singleton(entry));
            require(compiled.size() == 1, "Could not compile builtin scene for " + expectedEntry.getKey());
            PonderScene scene = compiled.get(0);
            require(value.sceneId.equals(scene.getId()), "Wrong scene id for " + expectedEntry.getKey());
            require(value.title.equals(scene.getTitle()), "Wrong title for " + value.sceneId + ": " + scene.getTitle());
            require(scene.getTotalTicks() == 640, "Wrong duration for " + value.sceneId + ": " + scene.getTotalTicks());
            require(scene.getKeyframes().equals(KEYFRAMES),
                "Wrong keyframes for " + value.sceneId + ": " + scene.getKeyframes());
        }
        report.setProperty("builtin.scenes", Integer.toString(found.size()));
        report.setProperty("builtin.duration", "640");
        report.setProperty("builtin.keyframes", KEYFRAMES.toString());
    }

    private static void verifyPositiveFixture(Properties report) {
        ResourceLocation component = new ResourceLocation("minecraft", "paper");
        StoryBoardEntry fixture = null;
        for (Map.Entry<ResourceLocation, StoryBoardEntry> entry
                : PonderIndex.getSceneAccess().getRegisteredEntries()) {
            if (component.equals(entry.getKey())
                && new ResourceLocation("ponder", "demo/basics").equals(entry.getValue().getSchematicLocation())) {
                fixture = entry.getValue();
                break;
            }
        }
        require(fixture != null, "Positive CraftTweaker fixture scene was not registered");
        List<PonderScene> compiled = PonderIndex.getSceneAccess()
            .compile(java.util.Collections.singleton(fixture));
        require(compiled.size() == 1, "Positive CraftTweaker fixture could not be compiled");
        PonderScene scene = compiled.get(0);
        require(new ResourceLocation("ponder_fixture", "advanced").equals(scene.getId()),
            "Unexpected fixture scene id " + scene.getId());
        require(scene.getTotalTicks() >= 20, "Fixture scene did not execute loop/function delays");
        scene.seek(scene.getTotalTicks());
        World world = scene.getWorld();
        TileEntity tile = world.getTileEntity(new BlockPos(3, 1, 2));
        require(tile != null, "Fixture scene lost the target chest tile entity");
        NBTTagCompound tileData = tile.writeToNBT(new NBTTagCompound());
        require("ponder_fixture".equals(tileData.getString("Lock")),
            "Fixture IData NBT did not update the target chest");
        report.setProperty("fixture.scene", scene.getId().toString());
        report.setProperty("fixture.duration", Integer.toString(scene.getTotalTicks()));
        report.setProperty("fixture.nbt", tileData.getString("Lock"));
    }

    private static Set<ResourceLocation> componentTags(ResourceLocation component) {
        java.util.LinkedHashSet<ResourceLocation> ids = new java.util.LinkedHashSet<ResourceLocation>();
        for (PonderTag tag : PonderIndex.getTagAccess().getTags(component))
            ids.add(tag.getId());
        return ids;
    }

    private static Map<ResourceLocation, ExpectedScene> expectedScenes() {
        Map<ResourceLocation, ExpectedScene> expected = new LinkedHashMap<ResourceLocation, ExpectedScene>();
        add(expected, "crafting_table", "ponder_basics", "demo/basics", "basics",
            "Ponder for Minecraft 1.12.2");
        add(expected, "chest", "chest_storage", "demo/storage", "storage",
            "Storing Items in a Chest");
        add(expected, "furnace", "furnace_smelting", "demo/smelting", "mechanics",
            "Smelting with a Furnace");
        add(expected, "piston", "piston_movement", "demo/piston", "mechanics",
            "Moving Blocks with a Piston");
        add(expected, "redstone_lamp", "redstone_lamp_power", "demo/redstone", "redstone",
            "Powering a Redstone Lamp");
        add(expected, "glass", "glass_render_layers", "demo/render_layers", "rendering",
            "Understanding Render Layers");
        add(expected, "water_bucket", "water_handling", "demo/fluids", "rendering",
            "Handling Water in a Scene");
        add(expected, "rail", "rail_minecart", "demo/rail", "mechanics",
            "Moving a Minecart along Rails");
        return expected;
    }

    private static void add(Map<ResourceLocation, ExpectedScene> target, String component, String scene,
                            String structure, String tag, String title) {
        target.put(new ResourceLocation("minecraft", component), new ExpectedScene(
            new ResourceLocation("ponder", scene),
            new ResourceLocation("ponder", structure),
            new ResourceLocation("ponder", tag),
            title));
    }

    private static void writeReport(Properties report) throws Exception {
        File config = Loader.instance().getConfigDir();
        File output = new File(config.getParentFile(), "ponder-fixture-harness.properties");
        try (OutputStream stream = new FileOutputStream(output)) {
            report.store(stream, "Ponder real CraftTweaker fixture verification");
        }
    }

    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }

    private static final class ExpectedScene {
        final ResourceLocation sceneId;
        final ResourceLocation structure;
        final ResourceLocation tag;
        final String title;

        ExpectedScene(ResourceLocation sceneId, ResourceLocation structure,
                      ResourceLocation tag, String title) {
            this.sceneId = sceneId;
            this.structure = structure;
            this.tag = tag;
            this.title = title;
        }
    }
}
