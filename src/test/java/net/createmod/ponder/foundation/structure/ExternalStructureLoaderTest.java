package net.createmod.ponder.foundation.structure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import org.junit.After;
import org.junit.Assume;
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
        PonderStructureLoader.setResourceProvider(null);
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

    @Test
    public void missingResourcePackEntryFallsBackToJar() throws Exception {
        PonderStructureLoader.setExternalRoot(temporary.newFolder("structures-resource-missing"));
        PonderStructureLoader.setResourceProvider(location -> {
            throw new FileNotFoundException(location.toString());
        });

        PonderStructure structure = new PonderStructureLoader()
            .load(new ResourceLocation("ponder", "demo/basics"));
        assertEquals(5, structure.getSize().getX());
    }

    @Test
    public void resourcePackReadFailureDoesNotFallBackToJar() throws Exception {
        PonderStructureLoader.setExternalRoot(temporary.newFolder("structures-resource-failure"));
        PonderStructureLoader.setResourceProvider(location -> {
            throw new IOException("fixture resource failure");
        });
        try {
            new PonderStructureLoader().load(new ResourceLocation("ponder", "demo/basics"));
            fail("Resource provider failures must be propagated");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("fixture resource failure"));
        }
    }

    @Test
    public void malformedResourcePackEntryDoesNotFallBackToJar() throws Exception {
        PonderStructureLoader.setExternalRoot(temporary.newFolder("structures-resource-malformed"));
        PonderStructureLoader.setResourceProvider(location ->
            new ByteArrayInputStream(new byte[] {1, 2, 3, 4}));
        try {
            new PonderStructureLoader().load(new ResourceLocation("ponder", "demo/basics"));
            fail("Malformed resource pack NBT must not be hidden by the jar fallback");
        } catch (IOException expected) {
            // The malformed stream itself is the expected result; Minecraft may omit its message.
        }
    }

    @Test
    public void rejectsNonRegularExternalTarget() throws Exception {
        File root = temporary.newFolder("structures-directory");
        Path target = root.toPath().resolve("external/demo.nbt");
        Files.createDirectories(target);
        PonderStructureLoader.setExternalRoot(root);
        try {
            new PonderStructureLoader().load(new ResourceLocation("external", "demo"));
            fail("Directory targets must be rejected");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("regular NBT file"));
        }
    }

    @Test
    public void rejectsOversizedExternalTarget() throws Exception {
        File root = temporary.newFolder("structures-oversized");
        Path target = root.toPath().resolve("external/demo.nbt");
        Files.createDirectories(target.getParent());
        try (java.io.RandomAccessFile file = new java.io.RandomAccessFile(target.toFile(), "rw")) {
            file.setLength(16L * 1024L * 1024L + 1L);
        }
        PonderStructureLoader.setExternalRoot(root);
        try {
            new PonderStructureLoader().load(new ResourceLocation("external", "demo"));
            fail("Oversized external structures must be rejected");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("exceeds"));
        }
    }

    @Test
    public void rejectsLinkedParentDirectoryEscapeWhenSupported() throws Exception {
        File root = temporary.newFolder("structures-linked");
        File outside = temporary.newFolder("structures-outside");
        Path namespaceLink = root.toPath().resolve("external");
        try {
            Files.createSymbolicLink(namespaceLink, outside.toPath());
        } catch (IOException | UnsupportedOperationException | SecurityException unavailable) {
            Assume.assumeNoException("Symbolic links are unavailable for this test user", unavailable);
        }
        Path target = outside.toPath().resolve("demo.nbt");
        copyBundledStructure(target);
        PonderStructureLoader.setExternalRoot(root);
        try {
            new PonderStructureLoader().load(new ResourceLocation("external", "demo"));
            fail("Linked parent directories must be rejected");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("symbolic link")
                || expected.getMessage().contains("junction"));
        }
    }

    private void copyBundledStructure(Path target) throws Exception {
        Files.createDirectories(target.getParent());
        InputStream source = getClass().getClassLoader()
            .getResourceAsStream("assets/ponder/ponder/demo/basics.nbt");
        if (source == null) throw new AssertionError("Bundled basics structure is missing");
        try {
            Files.copy(source, target);
        } finally {
            source.close();
        }
    }
}
