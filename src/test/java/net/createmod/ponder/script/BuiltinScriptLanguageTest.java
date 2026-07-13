package net.createmod.ponder.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import net.createmod.ponder.foundation.registration.PonderLocalization;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class BuiltinScriptLanguageTest {
    private static final String[] SCRIPTS = {
        "basics", "storage", "smelting", "piston",
        "redstone", "render_layers", "fluids", "rail"
    };
    private static final String[] TAGS = {
        "basics", "storage", "mechanics", "redstone", "rendering"
    };
    private static final Pattern SCENE = Pattern.compile(
        "SceneRegistry\\.create\\(\"[^\"]+\",\\s*\"([^\"]+)\",\\s*\"([^\"]+)\",\\s*\"([^\"]+)\"\\)");
    private static final Pattern TEXT = Pattern.compile(
        "scene\\.overlay\\.showText\\(\\d+,\\s*\"([^\"]+)\"");
    private static final Pattern FORMAT_TOKEN = Pattern.compile("%(?:\\d+\\$)?[a-zA-Z]");

    @Test
    public void languageResourcesMatchTheGeneratedZenScriptScenes() throws Exception {
        Map<String, String> english = loadLanguage("en_us");
        Map<String, String> chinese = loadLanguage("zh_cn");

        for (String scriptName : SCRIPTS) {
            String script = readText("assets/ponder/scripts/builtin/" + scriptName + ".zs");
            Matcher sceneMatcher = SCENE.matcher(script);
            if (!sceneMatcher.find())
                throw new AssertionError("Missing SceneRegistry.create in " + scriptName);
            ResourceLocation sceneId = new ResourceLocation(sceneMatcher.group(1));
            assertTranslation(english, chinese,
                PonderLocalization.langKeyForSpecific(sceneId, "header"), sceneMatcher.group(2));

            Matcher textMatcher = TEXT.matcher(script);
            int index = 1;
            while (textMatcher.find()) {
                assertTranslation(english, chinese,
                    PonderLocalization.langKeyForSpecific(sceneId, "text_" + index), textMatcher.group(1));
                index++;
            }
            assertEquals("Each builtin scene must expose four localized explanation texts: " + sceneId,
                5, index);
        }

        for (String tag : TAGS) {
            ResourceLocation id = new ResourceLocation("ponder", tag);
            assertTranslationPair(english, chinese, PonderLocalization.langKeyForTag(id));
            assertTranslationPair(english, chinese, PonderLocalization.langKeyForTagDescription(id));
        }
        assertTranslationPair(english, chinese,
            PonderLocalization.langKeyForShared(new ResourceLocation("ponder", "demo.controls")));
    }

    private static void assertTranslation(Map<String, String> english, Map<String, String> chinese,
                                          String key, String fallback) {
        assertTranslationPair(english, chinese, key);
        assertEquals("English resource drifted from generated ZenScript fallback for " + key,
            fallback, english.get(key));
    }

    private static void assertTranslationPair(Map<String, String> english, Map<String, String> chinese,
                                              String key) {
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

    private static Map<String, String> loadLanguage(String language) throws Exception {
        String path = "assets/ponder/lang/" + language + ".lang";
        InputStream stream = BuiltinScriptLanguageTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull("Missing language resource " + path, stream);
        try {
            Map<String, String> translations = new LinkedHashMap<String, String>();
            FMLCommonHandler.instance().loadLanguage(translations, stream);
            return translations;
        } finally {
            stream.close();
        }
    }

    private static String readText(String path) throws Exception {
        InputStream stream = BuiltinScriptLanguageTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull("Missing resource " + path, stream);
        try {
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = stream.read(buffer)) >= 0)
                output.write(buffer, 0, read);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            stream.close();
        }
    }
}
