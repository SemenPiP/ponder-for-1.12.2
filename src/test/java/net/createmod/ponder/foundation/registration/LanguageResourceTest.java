package net.createmod.ponder.foundation.registration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import net.minecraftforge.fml.common.FMLCommonHandler;

public class LanguageResourceTest {
    @Test
    public void parseEscapesMarkerUsesForgeUtf8PropertiesLoader() throws Exception {
        Map<String, String> english = load("en_us");
        assertEquals("Identify mode active.\nUnpause with [%1$s]", english.get("ponder.ui.identify_mode"));
        assertEquals("No Ponder scenes configured", english.get("ponder.ui.no_scenes_configured"));
        assertTrue(english.get("ponder.diagnostic.summary").contains("/ponder validate"));
        assertTrue(english.get("ponder.command.sync_status").contains("transfer=%3$s"));

        Map<String, String> chinese = load("zh_cn");
        assertEquals("思索", chinese.get("key.ponder.ponder"));
        assertEquals("未配置 Ponder 场景", chinese.get("ponder.ui.no_scenes_configured"));
        assertTrue(chinese.get("ponder.diagnostic.summary").contains("诊断问题"));
        assertTrue(chinese.get("ponder.command.sync_status").contains("传输=%3$s"));
        assertTrue(chinese.get("ponder.ui.identify_mode").contains("\n"));
    }

    private static Map<String, String> load(String language) throws Exception {
        String path = "assets/ponder/lang/" + language + ".lang";
        InputStream stream = LanguageResourceTest.class.getClassLoader().getResourceAsStream(path);
        assertTrue("Missing language resource " + path, stream != null);
        try {
            Map<String, String> translations = new LinkedHashMap<String, String>();
            assertNull("#PARSE_ESCAPES should make Forge consume " + path,
                FMLCommonHandler.instance().loadLanguage(translations, stream));
            return translations;
        } finally {
            stream.close();
        }
    }
}
