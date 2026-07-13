package net.createmod.ponder.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

import org.junit.Test;

import net.createmod.ponder.Ponder;
import net.createmod.ponder.PonderMod;
import net.createmod.ponder.mixin.PonderMixinLoader;
import net.minecraftforge.fml.common.Mod;

public class MixinBooterMetadataTest {
    private static final String SUPPORTED_DEPENDENCY = "required-after:mixinbooter@[9.1,)";
    private static final String CRAFTTWEAKER_DEPENDENCY = "required-after:crafttweaker@[4.1.20,)";

    @Test
    public void modAnnotationAllowsSupportedMixinBooterVersions() {
        Mod annotation = PonderMod.class.getAnnotation(Mod.class);
        assertNotNull(annotation);
        assertEquals(Ponder.MOD_ID, annotation.modid());
        assertTrue(annotation.dependencies().contains(SUPPORTED_DEPENDENCY));
        assertTrue(annotation.dependencies().contains(CRAFTTWEAKER_DEPENDENCY));
    }

    @Test
    public void processedModInfoAllowsSupportedMixinBooterVersions() throws Exception {
        Enumeration<URL> resources = getClass().getClassLoader().getResources("mcmod.info");
        String ponderMetadata = null;
        while (resources.hasMoreElements()) {
            String candidate = readUtf8(resources.nextElement());
            if (candidate.contains("\"modid\": \"" + Ponder.MOD_ID + "\"")) {
                ponderMetadata = candidate;
                break;
            }
        }
        assertNotNull("processed Ponder mcmod.info is missing", ponderMetadata);
        assertTrue(ponderMetadata.contains(SUPPORTED_DEPENDENCY));
        assertTrue(ponderMetadata.contains(CRAFTTWEAKER_DEPENDENCY));
    }

    @Test
    public void forgeIdentityDoesNotMoveTheContentNamespace() {
        assertEquals("ponder_legacy", Ponder.MOD_ID);
        assertEquals("ponder", Ponder.CONTENT_NAMESPACE);
        assertEquals("ponder", Ponder.asResource("demo/basics").getNamespace());
    }

    @Test
    public void earlyLoaderQueuesOnlyPonderMixinConfiguration() {
        assertEquals(1, new PonderMixinLoader().getMixinConfigs().size());
        assertEquals("mixins.ponder.json", new PonderMixinLoader().getMixinConfigs().get(0));
    }

    private static String readUtf8(URL resource) throws Exception {
        InputStream input = resource.openStream();
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            input.close();
        }
    }
}
