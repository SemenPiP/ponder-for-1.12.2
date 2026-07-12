package net.createmod.catnip.net;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import net.createmod.catnip.config.ConfigBase;
import net.createmod.catnip.config.ConfigRegistry;
import net.createmod.catnip.config.ConfigType;
import net.createmod.catnip.config.ui.ConfigHelper;

/** CommandBase-era parser and tab-completion adapter for Catnip configuration paths. */
public final class ConfigPathArgument {
    public static final List<String> EXAMPLES = Collections.unmodifiableList(Arrays.asList(
        "client", "ponder:common", "ponder:client.client.rainbowDebug"));
    private static final List<String> BASE_SUGGESTIONS = Arrays.asList("client", "common", "server");

    public ConfigPathArgument() {
    }

    public static ConfigPathArgument path() {
        return new ConfigPathArgument();
    }

    public ConfigHelper.Path parse(String input) {
        if (input == null || input.trim().isEmpty())
            throw new IllegalArgumentException("Unable to parse empty ConfigPath");
        String normalized = input.trim();
        if (normalized.indexOf(':') < 0) normalized = "ponder:" + normalized;
        return ConfigHelper.Path.parse(normalized);
    }

    public List<String> listSuggestions(String input) {
        String remaining = input == null ? "" : input.toLowerCase(Locale.ROOT);
        List<String> candidates = new ArrayList<String>();
        int colon = remaining.indexOf(':');
        if (colon < 0) {
            for (String type : BASE_SUGGESTIONS) if (type.startsWith(remaining)) candidates.add(type);
            for (String modId : ConfigRegistry.getModIds())
                if (modId.startsWith(remaining)) candidates.add(modId + ":");
        } else {
            String modId = remaining.substring(0, colon);
            String tail = remaining.substring(colon + 1);
            int dot = tail.indexOf('.');
            if (dot < 0) {
                for (String type : BASE_SUGGESTIONS) {
                    String candidate = modId + ":" + type;
                    if (candidate.startsWith(remaining)) candidates.add(candidate);
                }
            } else {
                String rawType = tail.substring(0, dot);
                try {
                    ConfigBase config = ConfigRegistry.get(modId, ConfigType.parse(rawType));
                    if (config != null) {
                        for (ConfigBase.CValue<?> value : config.getAllValues()) {
                            String candidate = modId + ":" + rawType + "." + value.getCategory()
                                + "." + value.getName();
                            if (candidate.toLowerCase(Locale.ROOT).startsWith(remaining)) candidates.add(candidate);
                        }
                    }
                } catch (IllegalArgumentException ignored) {
                    // Invalid side has no completions.
                }
            }
        }
        Collections.sort(candidates);
        return Collections.unmodifiableList(candidates);
    }
}
