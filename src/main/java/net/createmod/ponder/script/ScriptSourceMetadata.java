package net.createmod.ponder.script;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.minecraftforge.fml.common.Loader;

public final class ScriptSourceMetadata {
    private ScriptSourceMetadata() {
    }

    public static String normalize(String source) {
        if (source == null)
            return "";
        String value = source.trim().replace('\\', '/');
        if (value.isEmpty())
            return "";
        String suffix = "";
        int lineSeparator = value.lastIndexOf(':');
        if (lineSeparator > 1 && isDigits(value.substring(lineSeparator + 1))) {
            suffix = value.substring(lineSeparator);
            value = value.substring(0, lineSeparator);
        }
        try {
            File gameDirectory = Loader.instance().getConfigDir().getParentFile();
            Path game = gameDirectory.toPath().toAbsolutePath().normalize();
            Path candidate = Paths.get(value).toAbsolutePath().normalize();
            if (candidate.startsWith(game))
                value = game.relativize(candidate).toString().replace('\\', '/');
        } catch (RuntimeException ignored) {
        }
        int scripts = value.toLowerCase(java.util.Locale.ROOT).lastIndexOf("/scripts/");
        if (scripts >= 0)
            value = value.substring(scripts + 1);
        while (value.startsWith("./"))
            value = value.substring(2);
        try {
            if (Paths.get(value).isAbsolute())
                return "scripts/ponder/unknown" + suffix;
        } catch (RuntimeException invalidPath) {
            return "scripts/ponder/unknown" + suffix;
        }
        return value + suffix;
    }

    public static boolean isBuiltin(String source) {
        String normalized = normalize(source).toLowerCase(java.util.Locale.ROOT);
        return normalized.startsWith("scripts/ponder/builtin/")
            || normalized.contains("/scripts/ponder/builtin/");
    }

    private static boolean isDigits(String value) {
        if (value.isEmpty())
            return false;
        for (int i = 0; i < value.length(); i++)
            if (!Character.isDigit(value.charAt(i)))
                return false;
        return true;
    }
}
