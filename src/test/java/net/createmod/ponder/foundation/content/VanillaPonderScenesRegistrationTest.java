package net.createmod.ponder.foundation.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.BeforeClass;
import org.junit.Test;

import net.createmod.ponder.api.registration.StoryBoardEntry;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.PonderTag;
import net.createmod.ponder.foundation.registration.DefaultPonderSceneRegistrationHelper;
import net.createmod.ponder.foundation.registration.DefaultPonderTagRegistrationHelper;
import net.createmod.ponder.foundation.registration.DefaultSharedTextRegistrationHelper;
import net.createmod.ponder.foundation.registration.PonderLocalization;
import net.createmod.ponder.foundation.registration.PonderSceneRegistry;
import net.createmod.ponder.foundation.registration.PonderTagRegistry;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class VanillaPonderScenesRegistrationTest {
    private static final Pattern FORMAT_TOKEN = Pattern.compile("%(?:\\d+\\$)?[a-zA-Z]");

    @BeforeClass public static void bootstrapMinecraft() { Bootstrap.register(); }

    @Test
    public void registersEightUniqueVanillaScenesAndFiveIndexTags() {
        Registration registration = register();
        Map<ResourceLocation, ExpectedScene> expected = expectedScenes();

        assertEquals(8, registration.scenes.getRegisteredEntries().size());
        Set<ResourceLocation> sceneIds = new LinkedHashSet<ResourceLocation>();
        for (Map.Entry<ResourceLocation, StoryBoardEntry> registered
                : registration.scenes.getRegisteredEntries()) {
            ExpectedScene expectation = expected.get(registered.getKey());
            assertNotNull("Unexpected component " + registered.getKey(), expectation);
            StoryBoardEntry entry = registered.getValue();
            assertEquals(expectation.structure, entry.getSchematicLocation());
            assertEquals(Collections.singletonList(expectation.tag), entry.getTags());

            PonderScene compiled = PonderSceneRegistry.compileScene(registration.localization, entry, null);
            assertEquals(expectation.sceneId, compiled.getId());
            assertTrue("Duplicate scene id " + compiled.getId(), sceneIds.add(compiled.getId()));
            assertTrue("Scene is shorter than 30 seconds: " + compiled.getId(), compiled.getTotalTicks() >= 600);
            assertTrue("Scene is longer than 60 seconds: " + compiled.getId(), compiled.getTotalTicks() <= 1200);
            assertEquals("Each built-in scene should be exactly 32 seconds", 640, compiled.getTotalTicks());
            assertEquals("Each built-in scene should expose four stable steps", 4, compiled.getKeyframeCount());
            assertEquals("Built-in keyframes should follow the 150-tick step cadence",
                Arrays.asList(40, 190, 340, 490), compiled.getKeyframes());
            assertFalse("Timeline must not be empty", compiled.getKeyframes().isEmpty());
            assertFalse(compiled.getTitle().startsWith("missing scene text:"));
            for (int text = 1; text <= 4; text++)
                assertFalse(registration.localization.getSpecific(compiled.getId(), "text_" + text)
                    .startsWith("missing scene text:"));
        }
        assertEquals(expected.size(), sceneIds.size());

        assertEquals(5, registration.tags.getListedTags().size());
        assertEquals(tagIds(BasePonderPlugin.BASICS), tagIds(registration.tags.getTags(component("crafting_table"))));
        assertEquals(tagIds(BasePonderPlugin.STORAGE), tagIds(registration.tags.getTags(component("chest"))));
        assertEquals(tagIds(BasePonderPlugin.MECHANICS), tagIds(registration.tags.getTags(component("furnace"))));
        assertEquals(tagIds(BasePonderPlugin.MECHANICS), tagIds(registration.tags.getTags(component("piston"))));
        assertEquals(tagIds(BasePonderPlugin.MECHANICS), tagIds(registration.tags.getTags(component("rail"))));
        assertEquals(tagIds(BasePonderPlugin.REDSTONE), tagIds(registration.tags.getTags(component("redstone_lamp"))));
        assertEquals(tagIds(BasePonderPlugin.RENDERING), tagIds(registration.tags.getTags(component("glass"))));
        assertEquals(tagIds(BasePonderPlugin.RENDERING), tagIds(registration.tags.getTags(component("water_bucket"))));
    }

    @Test
    public void englishFallbackAndChineseResourcesCoverEveryBuiltInKey() throws Exception {
        Registration registration = register();
        Map<String, String> english = loadLanguage("en_us");
        Map<String, String> chinese = loadLanguage("zh_cn");

        for (ResourceLocation tag : Arrays.asList(BasePonderPlugin.BASICS, BasePonderPlugin.STORAGE,
                BasePonderPlugin.MECHANICS, BasePonderPlugin.REDSTONE, BasePonderPlugin.RENDERING)) {
            assertTranslationPair(english, chinese, PonderLocalization.langKeyForTag(tag));
            assertTranslationPair(english, chinese, PonderLocalization.langKeyForTagDescription(tag));
            assertEquals(registration.localization.getTagName(tag),
                english.get(PonderLocalization.langKeyForTag(tag)));
            assertEquals(registration.localization.getTagDescription(tag),
                english.get(PonderLocalization.langKeyForTagDescription(tag)));
        }

        for (Map.Entry<ResourceLocation, StoryBoardEntry> entry : registration.scenes.getRegisteredEntries()) {
            PonderScene compiled = PonderSceneRegistry.compileScene(registration.localization, entry.getValue(), null);
            for (String key : Arrays.asList("header", "text_1", "text_2", "text_3", "text_4")) {
                String languageKey = PonderLocalization.langKeyForSpecific(compiled.getId(), key);
                assertTranslationPair(english, chinese, languageKey);
                assertEquals("English resource drifted from the code fallback for " + languageKey,
                    registration.localization.getSpecific(compiled.getId(), key), english.get(languageKey));
            }
        }

        ResourceLocation controls = new ResourceLocation("ponder", "demo.controls");
        String controlsKey = PonderLocalization.langKeyForShared(controls);
        assertTranslationPair(english, chinese, controlsKey);
        assertEquals(registration.localization.getShared(controls), english.get(controlsKey));
    }

    @Test
    public void everyRegisteredSceneExecutesAndReplaysWithItsBundledStructure() {
        Registration registration = register();
        for (ResourceLocation component : expectedScenes().keySet()) {
            List<PonderScene> compiled = registration.scenes.compile(component);
            assertEquals("Expected one scene for " + component, 1, compiled.size());
            PonderScene scene = compiled.get(0);
            scene.seekToTime(scene.getTotalTicks());
            assertTrue("Scene did not finish: " + scene.getId(), scene.isFinished());

            scene.restart();
            scene.seekToTime(scene.getTotalTicks());
            assertTrue("Scene did not finish after replay: " + scene.getId(), scene.isFinished());
        }
    }

    private static void assertTranslationPair(Map<String, String> english, Map<String, String> chinese, String key) {
        assertNotNull("Missing English translation " + key, english.get(key));
        assertNotNull("Missing Chinese translation " + key, chinese.get(key));
        assertFalse("Blank English translation " + key, english.get(key).trim().isEmpty());
        assertFalse("Blank Chinese translation " + key, chinese.get(key).trim().isEmpty());
        assertEquals("Format placeholders differ for " + key,
            formatTokens(english.get(key)), formatTokens(chinese.get(key)));
    }

    private static List<String> formatTokens(String value) {
        List<String> tokens = new ArrayList<String>();
        Matcher matcher = FORMAT_TOKEN.matcher(value);
        while (matcher.find()) tokens.add(matcher.group());
        return tokens;
    }

    private static Registration register() {
        PonderLocalization localization = new PonderLocalization();
        PonderSceneRegistry scenes = new PonderSceneRegistry(localization);
        PonderTagRegistry tags = new PonderTagRegistry();
        BasePonderPlugin plugin = new BasePonderPlugin();
        VanillaPonderScenes.registerAll(new DefaultPonderSceneRegistrationHelper(plugin.getModId(), scenes));
        DefaultPonderTagRegistrationHelper tagHelper =
            new DefaultPonderTagRegistrationHelper(plugin.getModId(), tags, localization);
        tagHelper.registerTag(BasePonderPlugin.BASICS).title("Ponder Basics")
            .description("Learn the scene controls and the building blocks used by Ponder tutorials.")
            .item(Blocks.CRAFTING_TABLE).addToIndex().register();
        tagHelper.registerTag(BasePonderPlugin.STORAGE).title("Storage")
            .description("Inspect inventories, item entities and block entity data.")
            .item(Blocks.CHEST).addToIndex().register();
        tagHelper.registerTag(BasePonderPlugin.MECHANICS).title("Mechanics")
            .description("Follow explicit movement, processing and transport sequences.")
            .item(Blocks.PISTON).addToIndex().register();
        tagHelper.registerTag(BasePonderPlugin.REDSTONE).title("Redstone")
            .description("See powered states and signals change step by step.")
            .item(Blocks.REDSTONE_LAMP).addToIndex().register();
        tagHelper.registerTag(BasePonderPlugin.RENDERING).title("Rendering")
            .description("Compare render layers and transparent fluids in the virtual world.")
            .item(Blocks.GLASS).addToIndex().register();
        tagHelper.addToTag(BasePonderPlugin.BASICS).add(component("crafting_table"));
        tagHelper.addToTag(BasePonderPlugin.STORAGE).add(component("chest"));
        tagHelper.addToTag(BasePonderPlugin.MECHANICS)
            .add(component("furnace")).add(component("piston")).add(component("rail"));
        tagHelper.addToTag(BasePonderPlugin.REDSTONE).add(component("redstone_lamp"));
        tagHelper.addToTag(BasePonderPlugin.RENDERING)
            .add(component("glass")).add(component("water_bucket"));
        plugin.registerSharedText(new DefaultSharedTextRegistrationHelper(plugin.getModId(), localization));
        return new Registration(localization, scenes, tags);
    }

    private static Map<ResourceLocation, ExpectedScene> expectedScenes() {
        Map<ResourceLocation, ExpectedScene> expected = new LinkedHashMap<ResourceLocation, ExpectedScene>();
        expected.put(component("crafting_table"), expected("ponder_basics", "demo/basics", BasePonderPlugin.BASICS));
        expected.put(component("chest"), expected("chest_storage", "demo/storage", BasePonderPlugin.STORAGE));
        expected.put(component("furnace"), expected("furnace_smelting", "demo/smelting", BasePonderPlugin.MECHANICS));
        expected.put(component("piston"), expected("piston_movement", "demo/piston", BasePonderPlugin.MECHANICS));
        expected.put(component("redstone_lamp"),
            expected("redstone_lamp_power", "demo/redstone", BasePonderPlugin.REDSTONE));
        expected.put(component("glass"),
            expected("glass_render_layers", "demo/render_layers", BasePonderPlugin.RENDERING));
        expected.put(component("water_bucket"),
            expected("water_handling", "demo/fluids", BasePonderPlugin.RENDERING));
        expected.put(component("rail"), expected("rail_minecart", "demo/rail", BasePonderPlugin.MECHANICS));
        return expected;
    }

    private static ExpectedScene expected(String sceneId, String structure, ResourceLocation tag) {
        return new ExpectedScene(new ResourceLocation("ponder", sceneId),
            new ResourceLocation("ponder", structure), tag);
    }

    private static Set<ResourceLocation> tagIds(ResourceLocation... tags) {
        return new LinkedHashSet<ResourceLocation>(Arrays.asList(tags));
    }

    private static Set<ResourceLocation> tagIds(Collection<PonderTag> tags) {
        Set<ResourceLocation> ids = new LinkedHashSet<ResourceLocation>();
        for (PonderTag tag : tags) ids.add(tag.getId());
        return ids;
    }

    private static ResourceLocation component(String path) {
        return new ResourceLocation("minecraft", path);
    }

    private static Map<String, String> loadLanguage(String language) throws Exception {
        String path = "assets/ponder/lang/" + language + ".lang";
        InputStream stream = VanillaPonderScenesRegistrationTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull("Missing language resource " + path, stream);
        try {
            Map<String, String> translations = new LinkedHashMap<String, String>();
            FMLCommonHandler.instance().loadLanguage(translations, stream);
            return translations;
        } finally {
            stream.close();
        }
    }

    private static final class Registration {
        final PonderLocalization localization;
        final PonderSceneRegistry scenes;
        final PonderTagRegistry tags;

        Registration(PonderLocalization localization, PonderSceneRegistry scenes, PonderTagRegistry tags) {
            this.localization = localization;
            this.scenes = scenes;
            this.tags = tags;
        }
    }

    private static final class ExpectedScene {
        final ResourceLocation sceneId;
        final ResourceLocation structure;
        final ResourceLocation tag;

        ExpectedScene(ResourceLocation sceneId, ResourceLocation structure, ResourceLocation tag) {
            this.sceneId = sceneId;
            this.structure = structure;
            this.tag = tag;
        }
    }
}
