package net.createmod.ponder.script;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.function.Supplier;

import net.createmod.ponder.Ponder;

public final class BuiltinScriptInstaller {
    private static final String[] FILES = {
        "basics.zs", "storage.zs", "smelting.zs", "piston.zs",
        "redstone.zs", "render_layers.zs", "fluids.zs", "rail.zs"
    };
    private static final String RESOURCE_ROOT = "assets/ponder/scripts/builtin/";
    private static final String BUILTIN_DIRECTORY = "scripts/ponder/builtin";
    private static final String MARKER_FILE = "config/ponder/builtin-zs-generated.properties";
    private static final DateTimeFormatter UTC_STAMP = DateTimeFormatter
        .ofPattern("yyyyMMdd-HHmmss-SSS")
        .withZone(ZoneOffset.UTC);

    @FunctionalInterface
    interface ScriptCopier {
        void copy(String resource, Path target) throws IOException;
    }

    @FunctionalInterface
    interface MarkerWriter {
        void write(Path marker) throws IOException;
    }

    static volatile ScriptCopier scriptCopier = BuiltinScriptInstaller::copyResource;
    static volatile MarkerWriter markerWriter = BuiltinScriptInstaller::writeMarker;
    static volatile Supplier<String> utcStampSupplier = BuiltinScriptInstaller::currentUtcStamp;

    private BuiltinScriptInstaller() {
    }

    public static void installOnce(File gameDirectory) {
        if (gameDirectory == null) return;

        Path root = gameDirectory.toPath().toAbsolutePath().normalize();
        Path marker = root.resolve(MARKER_FILE).normalize();
        if (Files.exists(marker)) return;

        Path builtin = root.resolve(BUILTIN_DIRECTORY).normalize();
        Path staging = builtin.resolveSibling(builtin.getFileName().toString() + ".staging");
        Path backup = null;

        try {
            if (Files.exists(builtin)) {
                backup = uniqueBackupPath(builtin);
                moveAtomically(builtin, backup);
            }

            prepareEmptyDirectory(staging);
            writeScripts(staging);
            moveAtomically(staging, builtin);
            markerWriter.write(marker);
            Ponder.LOGGER.info("Generated Ponder builtin ZenScript files in {}", builtin);
        } catch (IOException exception) {
            rollbackInstall(builtin, staging, backup, marker);
            Ponder.LOGGER.error("Could not generate Ponder builtin ZenScript files", exception);
        }
    }

    private static void writeScripts(Path staging) throws IOException {
        for (String file : FILES) {
            Path target = staging.resolve(file).normalize();
            if (!target.startsWith(staging)) throw new IOException("Invalid builtin script path " + file);
            scriptCopier.copy(RESOURCE_ROOT + file, target);
        }
    }

    private static void copyResource(String resource, Path target) throws IOException {
        InputStream input = BuiltinScriptInstaller.class.getClassLoader().getResourceAsStream(resource);
        if (input == null) throw new IOException("Missing builtin script resource " + resource);
        Files.createDirectories(target.getParent());
        try (InputStream closeable = input) {
            Files.copy(closeable, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void writeMarker(Path marker) throws IOException {
        Files.createDirectories(marker.getParent());
        Properties properties = new Properties();
        properties.setProperty("format", "2");
        Path temporary = marker.resolveSibling(marker.getFileName().toString() + ".tmp");
        Files.deleteIfExists(temporary);
        try (OutputStream output = Files.newOutputStream(temporary)) {
            properties.store(output, "Ponder generated builtin ZenScript marker");
        }
        try {
            moveAtomically(temporary, marker);
        } catch (IOException exception) {
            Files.deleteIfExists(temporary);
            throw exception;
        }
    }

    private static void rollbackInstall(Path builtin, Path staging, Path backup, Path marker) {
        try {
            deleteRecursively(staging);
        } catch (IOException exception) {
            Ponder.LOGGER.warn("Could not clear staging directory {}", staging, exception);
        }

        try {
            deleteMarkerArtifacts(marker);
        } catch (IOException exception) {
            Ponder.LOGGER.warn("Could not clear builtin marker {}", marker, exception);
        }

        if (backup == null || !Files.exists(backup)) {
            try {
                deleteRecursively(builtin);
            } catch (IOException exception) {
                Ponder.LOGGER.warn("Could not clear builtin directory {}", builtin, exception);
            }
            return;
        }

        try {
            deleteRecursively(builtin);
            moveAtomically(backup, builtin);
        } catch (IOException exception) {
            Ponder.LOGGER.error("Could not restore previous Ponder builtin ZenScript files", exception);
        }
    }

    private static void deleteMarkerArtifacts(Path marker) throws IOException {
        Files.deleteIfExists(marker);
        Files.deleteIfExists(marker.resolveSibling(marker.getFileName().toString() + ".tmp"));
    }

    private static void prepareEmptyDirectory(Path directory) throws IOException {
        deleteRecursively(directory);
        Files.createDirectories(directory);
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) return;
        Files.walkFileTree(path, new java.nio.file.SimpleFileVisitor<Path>() {
            @Override
            public java.nio.file.FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs)
                throws IOException {
                Files.deleteIfExists(file);
                return java.nio.file.FileVisitResult.CONTINUE;
            }

            @Override
            public java.nio.file.FileVisitResult postVisitDirectory(Path dir, IOException exception)
                throws IOException {
                Files.deleteIfExists(dir);
                return java.nio.file.FileVisitResult.CONTINUE;
            }
        });
    }

    private static Path uniqueBackupPath(Path builtin) throws IOException {
        String base = builtin.getFileName().toString() + ".utc-" + utcStampSupplier.get();
        Path candidate = builtin.resolveSibling(base);
        int attempt = 0;
        while (Files.exists(candidate)) {
            attempt++;
            candidate = builtin.resolveSibling(base + "-" + attempt);
        }
        return candidate;
    }

    private static void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            throw new IOException("Atomic move not supported for " + source + " -> " + target, exception);
        }
    }

    private static String currentUtcStamp() {
        return UTC_STAMP.format(Instant.now());
    }
}
