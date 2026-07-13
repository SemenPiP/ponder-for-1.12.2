package net.createmod.ponder.foundation.structure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.minecraft.init.Bootstrap;
import net.minecraft.util.ResourceLocation;

public class ExternalStructureLoaderTest {
    @Rule public final TemporaryFolder temporary = new TemporaryFolder();

    @BeforeClass public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @After
    public void clearRoot() {
        PonderStructureLoader.setExternalRoot(null);
    }

    @Test
    public void externalStructureOverridesClasspathLocation() throws Exception {
        File root = temporary.newFolder("structures");
        File target = new File(root, "external/demo.nbt");
        assertFalse(target.exists());
        Files.createDirectories(target.toPath().getParent());
        InputStream source = getClass().getClassLoader()
            .getResourceAsStream("assets/ponder/ponder/demo/basics.nbt");
        if (source == null) throw new AssertionError("Bundled basics structure is missing");
        try {
            Files.copy(source, target.toPath());
        } finally {
            source.close();
        }

        PonderStructureLoader.setExternalRoot(root);
        PonderStructure structure = new PonderStructureLoader().load(new ResourceLocation("external", "demo"));
        assertEquals(5, structure.getSize().getX());
        assertEquals(5, structure.getSize().getZ());
    }

    @Test(expected = java.io.IOException.class)
    public void cacheDetectsSameLengthContentReplacementWithUnchangedTimestamp() throws Exception {
        File root = temporary.newFolder("structures-hash");
        File target = new File(root, "external/demo.nbt");
        Files.createDirectories(target.toPath().getParent());
        InputStream source = getClass().getClassLoader()
            .getResourceAsStream("assets/ponder/ponder/demo/basics.nbt");
        if (source == null) throw new AssertionError("Bundled basics structure is missing");
        try {
            Files.copy(source, target.toPath());
        } finally {
            source.close();
        }

        PonderStructureLoader.setExternalRoot(root);
        PonderStructureLoader loader = new PonderStructureLoader();
        loader.load(new ResourceLocation("external", "demo"));
        FileTime timestamp = Files.getLastModifiedTime(target.toPath());
        byte[] replaced = Files.readAllBytes(target.toPath());
        replaced[replaced.length / 2] ^= 0x5a;
        Files.write(target.toPath(), replaced);
        Files.setLastModifiedTime(target.toPath(), timestamp);

        loader.load(new ResourceLocation("external", "demo"));
    }
}
