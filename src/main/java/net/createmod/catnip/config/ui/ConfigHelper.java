package net.createmod.catnip.config.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import net.createmod.catnip.config.ConfigBase;
import net.createmod.catnip.config.ConfigPath;
import net.createmod.catnip.config.ConfigRegistry;
import net.createmod.catnip.config.ConfigType;
import net.createmod.catnip.config.ui.entries.SubMenuEntry;
import net.createmod.catnip.config.ui.entries.BooleanEntry;
import net.createmod.catnip.config.ui.entries.EnumEntry;
import net.createmod.catnip.config.ui.entries.NumberEntry;
import net.createmod.catnip.config.ui.entries.StringEntry;
import net.createmod.catnip.data.Pair;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.config.IConfigElement;

/** Bridge between Catnip's configuration DSL and Forge 1.12.2's GuiConfig. */
public final class ConfigHelper {
    public static final Pattern unitPattern = Pattern.compile("\\[(in .*)]");
    public static final Pattern annotationPattern = Pattern.compile("\\[@cui:([^:\\]]*)(?::([^\\]]*))?]");

    private ConfigHelper() {
    }

    public static boolean hasAnyConfig(String modId) {
        if (modId == null) return false;
        for (ConfigType type : ConfigType.values()) {
            if (ConfigRegistry.get(modId, type) != null) return true;
        }
        return false;
    }

    public static Map<ConfigType, ConfigBase> findConfigs(String modId) {
        EnumMap<ConfigType, ConfigBase> result = new EnumMap<ConfigType, ConfigBase>(ConfigType.class);
        for (ConfigType type : ConfigType.values()) {
            ConfigBase config = ConfigRegistry.get(modId, type);
            if (config != null) result.put(type, config);
        }
        return Collections.unmodifiableMap(result);
    }

    @Nullable
    public static ConfigBase findConfig(String modId, ConfigType type) {
        return ConfigRegistry.get(modId, type);
    }

    public static List<IConfigElement> getRootElements(String modId) {
        List<IConfigElement> elements = new ArrayList<IConfigElement>();
        for (Map.Entry<ConfigType, ConfigBase> entry : findConfigs(modId).entrySet()) {
            IConfigElement element = asRootElement(modId, entry.getKey(), entry.getValue());
            if (element != null) elements.add(element);
        }
        return elements;
    }

    public static List<IConfigElement> getElements(String modId, ConfigType type) {
        ConfigBase config = ConfigRegistry.get(modId, type);
        if (config == null) return Collections.emptyList();
        IConfigElement root = asRootElement(modId, type, config);
        return root == null ? Collections.<IConfigElement>emptyList() : root.getChildElements();
    }

    @Nullable
    public static IConfigElement asRootElement(String modId, ConfigType type, ConfigBase config) {
        if (config == null || config.specification == null) return null;
        ConfigCategory category = config.specification.getCategory(config.getName());
        category.setLanguageKey("catnip.config." + modId + "." + type.name().toLowerCase(Locale.ROOT));
        category.setConfigEntryClass(SubMenuEntry.class);
        prepareProperties(modId, config);
        return new ConfigElement(category).listCategoriesFirst(false);
    }

    private static void prepareProperties(String modId, ConfigBase config) {
        for (ConfigBase.CValue<?> value : config.getAllValues()) {
            Property property = value.getProperty();
            if (property == null) continue;
            String key = "catnip.config." + modId + "." + value.getCategory() + "." + value.getName();
            property.setLanguageKey(key);
            if (!property.isList()) {
                switch (property.getType()) {
                    case BOOLEAN:
                        property.setConfigEntryClass(BooleanEntry.class);
                        break;
                    case INTEGER:
                    case DOUBLE:
                        property.setConfigEntryClass(NumberEntry.class);
                        break;
                    case STRING:
                        String[] validValues = property.getValidValues();
                        property.setConfigEntryClass(validValues != null && validValues.length > 0
                            ? EnumEntry.class : StringEntry.class);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    public static ConfigBase.CValue<?> findValue(ConfigPath path) {
        return ConfigRegistry.require(path);
    }

    public static void setConfigValue(ConfigPath path, String value) throws InvalidValueException {
        try {
            ConfigRegistry.set(path, value);
        } catch (IllegalArgumentException exception) {
            throw new InvalidValueException(exception.getMessage(), exception);
        }
    }

    public static void saveAndReload(String modId) {
        for (ConfigBase config : findConfigs(modId).values()) {
            config.save();
            config.onReload();
        }
    }

    public static Pair<String, Map<String, String>> readMetadataFromComment(List<String> commentLines) {
        if (commentLines == null) throw new NullPointerException("commentLines");
        final AtomicReference<String> unit = new AtomicReference<String>();
        final Map<String, String> annotations = new LinkedHashMap<String, String>();
        java.util.Iterator<String> iterator = commentLines.iterator();
        while (iterator.hasNext()) {
            String line = iterator.next();
            if (line == null || line.trim().isEmpty()) {
                iterator.remove();
                continue;
            }
            Matcher annotation = annotationPattern.matcher(line.trim());
            if (annotation.matches()) {
                if (!annotations.containsKey(annotation.group(1))) {
                    annotations.put(annotation.group(1), annotation.group(2));
                }
                iterator.remove();
                continue;
            }
            Matcher unitMatcher = unitPattern.matcher(line.trim());
            if (unitMatcher.matches()) unit.set(unitMatcher.group(1));
        }
        return Pair.of(unit.get(), Collections.unmodifiableMap(annotations));
    }

    public static String toHumanReadable(String name) {
        if (name == null || name.isEmpty()) return "";
        String normalized = name.replace('_', ' ').replace('-', ' ');
        StringBuilder result = new StringBuilder(normalized.length() + 4);
        boolean capitalize = true;
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (Character.isUpperCase(c) && i > 0 && normalized.charAt(i - 1) != ' ') result.append(' ');
            result.append(capitalize ? Character.toUpperCase(c) : c);
            capitalize = c == ' ';
        }
        return result.toString();
    }

    /** Mutable compatibility path with the modern Catnip helper API. */
    public static final class Path {
        private String modId = "ponder";
        private ConfigType type = ConfigType.CLIENT;
        private String[] path = new String[0];

        public static Path parse(String serialized) {
            if (serialized == null) throw new IllegalArgumentException("path cannot be null");
            int colon = serialized.indexOf(':');
            String mod = colon < 0 ? "ponder" : serialized.substring(0, colon);
            String remainder = colon < 0 ? serialized : serialized.substring(colon + 1);
            String[] pieces = remainder.split("\\.");
            if (pieces.length < 1 || pieces[0].isEmpty())
                throw new IllegalArgumentException("path must include a config type");
            Path result = new Path().setID(mod).setType(ConfigType.parse(pieces[0]));
            return result.setPath(Arrays.copyOfRange(pieces, 1, pieces.length));
        }

        public Path setID(String modId) { this.modId = modId; return this; }
        public Path setType(ConfigType type) { this.type = type; return this; }
        public Path setPath(String[] path) { this.path = path == null ? new String[0] : path.clone(); return this; }
        public String getModID() { return modId; }
        public ConfigType getType() { return type; }
        public String[] getPath() { return path.clone(); }
        @Override public String toString() {
            StringBuilder result = new StringBuilder(modId).append(':')
                .append(type.name().toLowerCase(Locale.ROOT));
            for (String segment : path) result.append('.').append(segment);
            return result.toString();
        }
    }

    public static final class ConfigChange {
        private final Object value;
        private final Map<String, String> annotations;
        public ConfigChange(Object value) { this(value, Collections.<String, String>emptyMap()); }
        public ConfigChange(Object value, Map<String, String> annotations) {
            this.value = value;
            this.annotations = Collections.unmodifiableMap(new HashMap<String, String>(annotations));
        }
        public Object getValue() { return value; }
        public Map<String, String> getAnnotations() { return annotations; }
    }

    public static class InvalidValueException extends Exception {
        private static final long serialVersionUID = 1L;
        public InvalidValueException(String message, Throwable cause) { super(message, cause); }
    }
}
