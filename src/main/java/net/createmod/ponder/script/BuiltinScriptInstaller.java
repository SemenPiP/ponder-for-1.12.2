package net.createmod.ponder.script;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

import net.createmod.ponder.Ponder;

public final class BuiltinScriptInstaller {
    private static final String[] FILES = {
        "basics.zs", "storage.zs", "smelting.zs", "piston.zs",
        "redstone.zs", "render_layers.zs", "fluids.zs", "rail.zs"
    };
    private static final String RESOURCE_ROOT = "assets/ponder/scripts/builtin/";

    private BuiltinScriptInstaller() {
    }

    public static void installOnce(File gameDirectory) {
        if (gameDirectory == null) return;
        Path marker = gameDirectory.toPath().resolve("config").resolve("ponder")
            .resolve("builtin-zs-generated.properties").toAbsolutePath().normalize();
        if (Files.exists(marker)) return;
        Path scripts = gameDirectory.toPath().resolve("scripts").resolve("ponder").resolve("builtin")
            .toAbsolutePath().normalize();
        try {
            Files.createDirectories(scripts);
            for (String file : FILES) {
                Path target = scripts.resolve(file).normalize();
                if (!target.startsWith(scripts)) throw new IOException("Invalid builtin script path " + file);
                if (!Files.exists(target)) copyResource(RESOURCE_ROOT + file, target);
            }
            Files.createDirectories(marker.getParent());
            Properties properties = new Properties();
            properties.setProperty("format", "1");
            properties.setProperty("version", "1.1.0");
            Path temporary = marker.resolveSibling(marker.getFileName().toString() + ".tmp");
            try (OutputStream output = Files.newOutputStream(temporary)) {
                properties.store(output, "Ponder generated builtin ZenScript marker");
            }
            moveAtomically(temporary, marker);
            Ponder.LOGGER.info("Generated Ponder builtin ZenScript files in {}", scripts);
        } catch (IOException exception) {
            Ponder.LOGGER.error("Could not generate Ponder builtin ZenScript files", exception);
        }
    }

    private static void copyResource(String resource, Path target) throws IOException {
        InputStream input = BuiltinScriptInstaller.class.getClassLoader().getResourceAsStream(resource);
        if (input == null) throw new IOException("Missing builtin script resource " + resource);
        Path temporary = target.resolveSibling(target.getFileName().toString() + ".tmp");
        try (InputStream closeable = input) {
            Files.copy(closeable, temporary, StandardCopyOption.REPLACE_EXISTING);
        }
        moveAtomically(temporary, target);
    }

    private static void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
