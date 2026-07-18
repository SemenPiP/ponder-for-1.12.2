package net.createmod.ponder.script;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.createmod.ponder.Ponder;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticSeverity;
import net.createmod.ponder.api.diagnostic.PonderSceneSource;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.util.ResourceLocation;

/**
 * Loads deterministic JSON scene packs into the same validated IR used by
 * ZenScript. JSON files are reloadable; ZenScript remains startup-only.
 */
public final class PonderJsonLoader {
    public static final int FORMAT_VERSION = 1;
    public static final int MAX_FILES = 1024;
    public static final long MAX_FILE_BYTES = 1024 * 1024;
    public static final long MAX_TOTAL_BYTES = ScriptSceneSnapshot.MAX_UNCOMPRESSED_BYTES;

    private static final Set<String> PACK_FIELDS = fields(
        "$schema", "format", "id", "scenes", "tags", "sharedText", "indexExclusions");
    private static final Set<String> SCENE_FIELDS = fields(
        "id", "component", "title", "structure", "tags", "clientOnly", "instructions");
    private static final Set<String> TAG_FIELDS = fields(
        "id", "icon", "title", "description", "indexed", "components");
    private static final Map<String, InstructionSpec> OPERATIONS = operations();

    private static File root;
    private static Map<String, Pack> lastGood = Collections.emptyMap();
    private static Runnable reloadAction = PonderIndex::reload;

    private PonderJsonLoader() {
    }

    public static synchronized void setRoot(File directory) {
        root = directory;
        lastGood = Collections.emptyMap();
    }

    static synchronized void setReloadActionForTest(Runnable action) {
        reloadAction = action == null ? PonderIndex::reload : action;
    }

    public static synchronized ReloadResult reload() {
        if (root == null)
            return new ReloadResult(0, 0, 0, 0);
        ScanResult scan = scan(root);
        apply(scan.snapshot);
        lastGood = Collections.unmodifiableMap(new LinkedHashMap<String, Pack>(scan.selected));
        Ponder.LOGGER.info("Loaded {} Ponder JSON pack(s), {} scene(s), {} warning(s), {} error(s)",
            scan.selected.size(), scan.snapshot.scenes.size(), scan.warnings, scan.errors);
        return new ReloadResult(scan.selected.size(), scan.snapshot.scenes.size(),
            scan.warnings, scan.errors);
    }

    private static ScanResult scan(File directory) {
        Path rootPath = directory.toPath().toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootPath);
        } catch (IOException failure) {
            throw new IllegalStateException("Could not create Ponder JSON pack directory " + rootPath, failure);
        }
        List<Path> files = new ArrayList<Path>();
        try (Stream<Path> stream = Files.walk(rootPath)) {
            stream.filter(path -> path.getFileName().toString().endsWith(".ponder.json"))
                .forEach(files::add);
        } catch (IOException failure) {
            throw new IllegalStateException("Could not scan Ponder JSON packs", failure);
        }
        Collections.sort(files, Comparator.comparing(path -> relative(rootPath, path)));
        if (files.size() > MAX_FILES)
            throw new IllegalStateException("Ponder JSON pack count exceeds " + MAX_FILES);

        SnapshotBuilder builder = new SnapshotBuilder();
        Map<String, Pack> selected = new LinkedHashMap<String, Pack>();
        int warnings = 0;
        int errors = 0;
        long totalBytes = 0;
        for (Path file : files) {
            String source = "scripts/ponder/packs/" + relative(rootPath, file);
            Pack parsed = null;
            try {
                if (Files.isSymbolicLink(file) || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
                    throw new IllegalArgumentException("JSON pack must be a regular non-symlink file");
                Path real = file.toRealPath(LinkOption.NOFOLLOW_LINKS);
                Path realRoot = rootPath.toRealPath(LinkOption.NOFOLLOW_LINKS);
                if (!real.startsWith(realRoot))
                    throw new IllegalArgumentException("JSON pack escaped scripts/ponder/packs");
                long size = Files.size(file);
                if (size > MAX_FILE_BYTES)
                    throw new IllegalArgumentException("JSON pack exceeds " + MAX_FILE_BYTES + " bytes");
                totalBytes += size;
                if (totalBytes > MAX_TOTAL_BYTES)
                    throw new IllegalArgumentException("JSON packs exceed " + MAX_TOTAL_BYTES + " bytes");
                parsed = parse(file, source);
                builder.add(parsed);
                selected.put(source, parsed);
                continue;
            } catch (RuntimeException | IOException failure) {
                errors++;
                record("json.pack_invalid", PonderDiagnosticSeverity.ERROR,
                    source + ": " + message(failure), null, -1);
            }
            Pack previous = lastGood.get(source);
            if (previous != null) {
                try {
                    builder.add(previous);
                    selected.put(source, previous);
                    warnings++;
                    record("json.pack_stale", PonderDiagnosticSeverity.WARNING,
                        source + ": retaining the last-known-good pack", null, -1);
                } catch (RuntimeException conflict) {
                    errors++;
                    record("json.pack_stale_conflict", PonderDiagnosticSeverity.ERROR,
                        source + ": last-known-good pack could not be retained: " + message(conflict),
                        null, -1);
                }
            }
        }
        return new ScanResult(builder.build(), selected, warnings, errors);
    }

    private static Pack parse(Path file, String source) throws IOException {
        JsonElement parsed;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            parsed = new JsonParser().parse(reader);
        }
        JsonObject root = object(parsed, source);
        rejectUnknown(root, PACK_FIELDS, source);
        int format = integer(root, "format", source);
        if (format != FORMAT_VERSION)
            throw new IllegalArgumentException("Unsupported Ponder JSON format " + format);
        ResourceLocation packId = id(string(root, "id", source), "pack id");

        List<ScriptSceneDefinition> scenes = new ArrayList<ScriptSceneDefinition>();
        JsonArray sceneArray = array(root, "scenes");
        for (int i = 0; i < sceneArray.size(); i++)
            scenes.add(scene(object(sceneArray.get(i), pointer(source, "scenes", i)),
                pointer(source, "scenes", i)));

        List<ScriptTagDefinition> tags = new ArrayList<ScriptTagDefinition>();
        JsonArray tagArray = array(root, "tags");
        for (int i = 0; i < tagArray.size(); i++)
            tags.add(tag(object(tagArray.get(i), pointer(source, "tags", i)),
                pointer(source, "tags", i)));

        Map<String, String> sharedText = new LinkedHashMap<String, String>();
        JsonObject text = optionalObject(root, "sharedText");
        if (text != null) {
            for (Map.Entry<String, JsonElement> entry : text.entrySet()) {
                String value = primitiveString(entry.getValue(), source + "#/sharedText/" + entry.getKey());
                if (sharedText.put(entry.getKey(), value) != null)
                    throw new IllegalArgumentException("Duplicate shared text key " + entry.getKey());
            }
        }

        List<ResourceLocation> exclusions = new ArrayList<ResourceLocation>();
        JsonArray exclusionArray = array(root, "indexExclusions");
        for (int i = 0; i < exclusionArray.size(); i++)
            exclusions.add(id(primitiveString(exclusionArray.get(i),
                pointer(source, "indexExclusions", i)), "index exclusion"));
        return new Pack(packId, source, scenes, tags, sharedText, exclusions);
    }

    private static ScriptSceneDefinition scene(JsonObject value, String source) {
        rejectUnknown(value, SCENE_FIELDS, source);
        ResourceLocation sceneId = id(string(value, "id", source), "scene id");
        ResourceLocation component = id(string(value, "component", source), "component id");
        String title = string(value, "title", source);
        ResourceLocation structure = id(string(value, "structure", source), "structure id");
        List<ResourceLocation> tags = ids(array(value, "tags"), source + "#/tags");
        boolean clientOnly = optionalBoolean(value, "clientOnly", false, source);
        JsonArray encoded = array(value, "instructions");
        List<ScriptInstruction> instructions = new ArrayList<ScriptInstruction>(encoded.size());
        for (int i = 0; i < encoded.size(); i++)
            instructions.add(instruction(object(encoded.get(i), pointer(source, "instructions", i)),
                pointer(source, "instructions", i)));
        return new ScriptSceneDefinition(component, sceneId, title, structure, tags,
            instructions, clientOnly, source, PonderSceneSource.LOCAL_JSON);
    }

    private static ScriptTagDefinition tag(JsonObject value, String source) {
        rejectUnknown(value, TAG_FIELDS, source);
        return new ScriptTagDefinition(
            id(string(value, "id", source), "tag id"),
            id(string(value, "icon", source), "tag icon"),
            string(value, "title", source),
            string(value, "description", source),
            optionalBoolean(value, "indexed", true, source),
            ids(array(value, "components"), source + "#/components"));
    }

    private static ScriptInstruction instruction(JsonObject value, String source) {
        String requested = string(value, "op", source);
        InstructionSpec spec = OPERATIONS.get(requested);
        if (spec == null)
            throw new IllegalArgumentException("Unknown Ponder JSON operation " + requested + " at " + source);
        for (Map.Entry<String, JsonElement> field : value.entrySet())
            if (!"op".equals(field.getKey()) && !spec.fields.contains(field.getKey()))
                throw new IllegalArgumentException("Unknown field " + field.getKey()
                    + " for " + requested + " at " + source);
        NBTTagCompound data = new NBTTagCompound();
        for (Map.Entry<String, JsonElement> entry : value.entrySet()) {
            String key = entry.getKey();
            if ("op".equals(key))
                continue;
            if ("selection".equals(key)) {
                data.setTag(key, selection(object(entry.getValue(), source + "/selection"), source + "/selection"));
            } else if ("nbt".equals(key) || "payload".equals(key)) {
                data.setTag(key, snbtCompound(primitiveString(entry.getValue(), source + "/" + key),
                    source + "/" + key));
            } else if ("params".equals(key)) {
                data.setTag(key, stringList(entry.getValue(), source + "/params"));
            } else {
                putPrimitive(data, key, entry.getValue(), source + "/" + key);
            }
        }
        return new ScriptInstruction(spec.internalOperation, data);
    }

    private static NBTTagCompound selection(JsonObject value, String source) {
        String type = string(value, "type", source);
        NBTTagCompound result = new NBTTagCompound();
        if ("position".equals(type)) {
            rejectUnknown(value, fields("type", "pos"), source);
            result.setString("type", "position");
            result.setIntArray("values", intVector(value.get("pos"), source + "/pos"));
        } else if ("from_to".equals(type)) {
            rejectUnknown(value, fields("type", "from", "to"), source);
            int[] from = intVector(value.get("from"), source + "/from");
            int[] to = intVector(value.get("to"), source + "/to");
            result.setString("type", "from_to");
            result.setIntArray("values", concat(from, to));
        } else if ("column".equals(type)) {
            rejectUnknown(value, fields("type", "x", "z"), source);
            result.setString("type", "column");
            result.setIntArray("values", new int[] { integer(value, "x", source), integer(value, "z", source) });
        } else if ("layer".equals(type) || "layers_from".equals(type)) {
            rejectUnknown(value, fields("type", "y"), source);
            result.setString("type", type);
            result.setIntArray("values", new int[] { integer(value, "y", source) });
        } else if ("layers".equals(type)) {
            rejectUnknown(value, fields("type", "y", "height"), source);
            result.setString("type", type);
            result.setIntArray("values",
                new int[] { integer(value, "y", source), integer(value, "height", source) });
        } else if ("cuboid".equals(type)) {
            rejectUnknown(value, fields("type", "origin", "offset"), source);
            result.setString("type", type);
            result.setIntArray("values", concat(
                intVector(value.get("origin"), source + "/origin"),
                intVector(value.get("offset"), source + "/offset")));
        } else if ("everywhere".equals(type)) {
            rejectUnknown(value, fields("type"), source);
            result.setString("type", type);
            result.setIntArray("values", new int[0]);
        } else if ("structure_group".equals(type)) {
            rejectUnknown(value, fields("type", "name"), source);
            result.setString("type", "everywhere");
            result.setIntArray("values", new int[0]);
            result.setString("structure_group", string(value, "name", source));
        } else {
            throw new IllegalArgumentException("Unknown selection type " + type + " at " + source);
        }
        return result;
    }

    private static void apply(Snapshot replacement) {
        List<ScriptSceneDefinition> oldScenes = ScriptSceneRegistry.jsonSnapshot();
        Collection<ScriptTagDefinition> oldTags = ScriptTagRegistry.jsonSnapshot();
        Map<String, String> oldSharedText = ScriptSharedText.jsonSnapshot();
        List<ResourceLocation> oldExclusions = ScriptIndex.jsonSnapshot();
        try {
            ScriptSceneRegistry.replaceJsonScenes(replacement.scenes);
            ScriptTagRegistry.replaceJson(replacement.tags);
            ScriptSharedText.replaceJson(replacement.sharedText);
            ScriptIndex.replaceJson(replacement.indexExclusions);
            reloadAction.run();
        } catch (RuntimeException failure) {
            ScriptSceneRegistry.replaceJsonScenes(oldScenes);
            ScriptTagRegistry.replaceJson(oldTags);
            ScriptSharedText.replaceJson(oldSharedText);
            ScriptIndex.replaceJson(oldExclusions);
            try {
                reloadAction.run();
            } catch (RuntimeException rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
            }
            throw new IllegalStateException("Could not apply Ponder JSON packs: " + message(failure), failure);
        }
    }

    private static Map<String, InstructionSpec> operations() {
        Map<String, InstructionSpec> result = new LinkedHashMap<String, InstructionSpec>();
        op(result, "scene.configure_base_plate", "configure_base_plate", "x", "z", "size");
        op(result, "scene.show_base_plate", "show_base_plate");
        op(result, "scene.remove_shadow", "remove_shadow");
        op(result, "scene.scale", "scale", "value");
        op(result, "scene.offset_y", "offset_y", "value");
        op(result, "scene.idle", "idle", "ticks");
        op(result, "scene.rotate_camera", "rotate_camera", "degrees");
        op(result, "scene.keyframe", "keyframe");
        op(result, "scene.lazy_keyframe", "lazy_keyframe");
        op(result, "scene.finish", "finish");
        op(result, "scene.next_up", "next_up", "enabled");
        op(result, "scene.move_poi", "move_poi", "x", "y", "z");
        op(result, "scene.custom", "custom", "codec", "payload");

        op(result, "world.show_section", "show_section", "selection", "direction");
        op(result, "world.hide_section", "hide_section", "selection", "direction");
        op(result, "world.restore_blocks", "restore_blocks", "selection");
        op(result, "world.show_independent", "show_independent", "handle", "selection", "direction");
        op(result, "world.show_independent_immediate", "show_independent_immediate", "handle", "selection");
        op(result, "world.hide_independent", "hide_independent", "handle", "direction");
        op(result, "world.make_independent", "make_independent", "handle", "selection");
        op(result, "world.show_section_merge", "show_section_merge", "handle", "selection", "direction");
        op(result, "world.glue_block", "glue_block", "handle", "x", "y", "z", "direction");
        op(result, "world.move_section", "move_section", "handle", "x", "y", "z", "duration");
        op(result, "world.rotate_section", "rotate_section", "handle", "x", "y", "z", "duration");
        op(result, "world.center_section", "center_section", "handle", "x", "y", "z");
        op(result, "world.stabilize_section", "stabilize_section", "handle", "x", "y", "z");
        op(result, "world.set_block", "set_block", "x", "y", "z", "state", "particles");
        op(result, "world.set_blocks", "set_blocks", "selection", "state", "particles");
        op(result, "world.destroy_block", "destroy_block", "x", "y", "z");
        op(result, "world.break_progress", "break_progress", "x", "y", "z");
        op(result, "world.cycle_property", "cycle_property", "x", "y", "z", "property");
        op(result, "world.toggle_redstone", "toggle_redstone", "selection");
        op(result, "world.create_item", "create_item",
            "handle", "x", "y", "z", "mx", "my", "mz", "item", "count", "meta");
        op(result, "world.move_item", "move_item", "handle", "x", "y", "z", "duration");
        op(result, "world.set_item_visible", "set_item_visible", "handle", "visible");
        op(result, "world.remove_item", "remove_item", "handle");
        op(result, "world.create_minecart", "create_minecart", "handle", "x", "y", "z", "angle", "type");
        op(result, "world.move_minecart", "move_minecart", "handle", "x", "y", "z", "duration");
        op(result, "world.rotate_minecart", "rotate_minecart", "handle", "angle", "duration");
        op(result, "world.hide_minecart", "hide_minecart", "handle", "direction");
        op(result, "world.create_parrot", "create_parrot", "handle", "x", "y", "z", "pose");
        op(result, "world.change_parrot_pose", "change_parrot_pose", "handle", "pose");
        op(result, "world.move_parrot", "move_parrot", "handle", "x", "y", "z", "duration");
        op(result, "world.rotate_parrot", "rotate_parrot", "handle", "x", "y", "z", "duration");
        op(result, "world.hide_parrot", "hide_parrot", "handle", "direction");
        op(result, "world.tile_nbt", "tile_nbt", "selection", "nbt", "replace", "redraw");

        op(result, "overlay.show_text", "show_text",
            "duration", "text", "x", "y", "z", "color", "near", "keyframe");
        op(result, "overlay.show_shared_text", "show_shared_text",
            "duration", "key", "params", "x", "y", "z", "color", "near", "keyframe");
        op(result, "overlay.show_independent_text", "show_independent_text",
            "duration", "text", "y", "color", "keyframe");
        op(result, "overlay.show_controls", "show_controls",
            "duration", "x", "y", "z", "pointing", "action", "item");
        op(result, "overlay.show_line", "show_line",
            "color", "x1", "y1", "z1", "x2", "y2", "z2", "duration", "big");
        op(result, "overlay.show_outline", "show_outline", "color", "slot", "selection", "duration");
        op(result, "overlay.show_outline_text", "show_outline_text",
            "duration", "text", "color", "selection", "keyframe");
        op(result, "overlay.show_bounding_box", "show_bounding_box",
            "color", "slot", "minX", "minY", "minZ", "maxX", "maxY", "maxZ", "duration");
        op(result, "overlay.show_scroll_input", "show_scroll_input", "x", "y", "z", "side", "duration");
        op(result, "overlay.show_centered_scroll_input", "show_centered_scroll_input",
            "x", "y", "z", "side", "duration");
        op(result, "overlay.show_repeater_scroll_input", "show_repeater_scroll_input",
            "x", "y", "z", "duration");
        op(result, "overlay.show_filter_slot_input", "show_filter_slot_input",
            "x", "y", "z", "side", "duration");

        op(result, "effects.indicate_redstone", "indicate_redstone", "x", "y", "z");
        op(result, "effects.indicate_success", "indicate_success", "x", "y", "z");
        op(result, "effects.redstone_particles", "redstone_particles", "x", "y", "z", "color", "amount");
        op(result, "effects.particles", "particles",
            "x", "y", "z", "type", "mx", "my", "mz", "amount", "cycles");
        op(result, "effects.particles_within_block", "particles_within_block",
            "x", "y", "z", "type", "mx", "my", "mz", "amount", "cycles");
        return Collections.unmodifiableMap(result);
    }

    private static void op(Map<String, InstructionSpec> operations, String publicName,
                           String internalName, String... fields) {
        InstructionSpec spec = new InstructionSpec(internalName, PonderJsonLoader.fields(fields));
        operations.put(publicName, spec);
        operations.put(internalName, spec);
    }

    private static void putPrimitive(NBTTagCompound target, String key, JsonElement value, String source) {
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive())
            throw new IllegalArgumentException("Expected primitive value at " + source);
        if (value.getAsJsonPrimitive().isBoolean()) {
            target.setBoolean(key, value.getAsBoolean());
        } else if (value.getAsJsonPrimitive().isString()) {
            target.setString(key, value.getAsString());
        } else if (value.getAsJsonPrimitive().isNumber()) {
            String number = value.getAsString();
            if (number.indexOf('.') >= 0 || number.indexOf('e') >= 0 || number.indexOf('E') >= 0) {
                double parsed = Double.parseDouble(number);
                if (!Double.isFinite(parsed))
                    throw new IllegalArgumentException("Non-finite number at " + source);
                target.setDouble(key, parsed);
            } else {
                long parsed = Long.parseLong(number);
                if (parsed >= Integer.MIN_VALUE && parsed <= Integer.MAX_VALUE)
                    target.setInteger(key, (int) parsed);
                else
                    target.setLong(key, parsed);
            }
        } else {
            throw new IllegalArgumentException("Unsupported primitive at " + source);
        }
    }

    private static NBTTagCompound snbtCompound(String value, String source) {
        try {
            NBTBase parsed = JsonToNBT.getTagFromJson(value);
            if (!(parsed instanceof NBTTagCompound))
                throw new IllegalArgumentException("SNBT value must be a compound at " + source);
            return (NBTTagCompound) parsed;
        } catch (Exception failure) {
            throw new IllegalArgumentException("Invalid SNBT at " + source + ": " + message(failure), failure);
        }
    }

    private static NBTTagList stringList(JsonElement value, String source) {
        if (value == null || !value.isJsonArray())
            throw new IllegalArgumentException("Expected string array at " + source);
        NBTTagList result = new NBTTagList();
        JsonArray values = value.getAsJsonArray();
        for (int i = 0; i < values.size(); i++)
            result.appendTag(new NBTTagString(primitiveString(values.get(i), source + "/" + i)));
        return result;
    }

    private static int[] intVector(JsonElement value, String source) {
        if (value == null || !value.isJsonArray() || value.getAsJsonArray().size() != 3)
            throw new IllegalArgumentException("Expected three-integer vector at " + source);
        int[] result = new int[3];
        for (int i = 0; i < 3; i++) {
            JsonElement element = value.getAsJsonArray().get(i);
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber())
                throw new IllegalArgumentException("Expected integer at " + source + "/" + i);
            String number = element.getAsString();
            if (number.contains(".") || number.contains("e") || number.contains("E"))
                throw new IllegalArgumentException("Expected integer at " + source + "/" + i);
            result[i] = Integer.parseInt(number);
        }
        return result;
    }

    private static List<ResourceLocation> ids(JsonArray array, String source) {
        List<ResourceLocation> result = new ArrayList<ResourceLocation>(array.size());
        for (int i = 0; i < array.size(); i++)
            result.add(id(primitiveString(array.get(i), source + "/" + i), "resource id"));
        return result;
    }

    private static ResourceLocation id(String value, String label) {
        if (value == null || value.trim().isEmpty() || value.length() > 256)
            throw new IllegalArgumentException("Invalid " + label);
        return new ResourceLocation(value);
    }

    private static JsonObject object(JsonElement value, String source) {
        if (value == null || !value.isJsonObject())
            throw new IllegalArgumentException("Expected JSON object at " + source);
        return value.getAsJsonObject();
    }

    private static JsonObject optionalObject(JsonObject value, String key) {
        JsonElement member = value.get(key);
        if (member == null || member.isJsonNull())
            return null;
        return object(member, key);
    }

    private static JsonArray array(JsonObject value, String key) {
        JsonElement member = value.get(key);
        if (member == null || member.isJsonNull())
            return new JsonArray();
        if (!member.isJsonArray())
            throw new IllegalArgumentException("Expected JSON array at " + key);
        return member.getAsJsonArray();
    }

    private static String string(JsonObject value, String key, String source) {
        JsonElement member = value.get(key);
        if (member == null)
            throw new IllegalArgumentException("Missing " + key + " at " + source);
        return primitiveString(member, source + "/" + key);
    }

    private static String primitiveString(JsonElement value, String source) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
            throw new IllegalArgumentException("Expected string at " + source);
        String result = value.getAsString();
        if (result.length() > ScriptSceneSnapshot.MAX_TEXT_LENGTH)
            throw new IllegalArgumentException("String exceeds "
                + ScriptSceneSnapshot.MAX_TEXT_LENGTH + " characters at " + source);
        return result;
    }

    private static int integer(JsonObject value, String key, String source) {
        JsonElement member = value.get(key);
        if (member == null || !member.isJsonPrimitive() || !member.getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException("Expected integer " + key + " at " + source);
        String number = member.getAsString();
        if (number.contains(".") || number.contains("e") || number.contains("E"))
            throw new IllegalArgumentException("Expected integer " + key + " at " + source);
        return Integer.parseInt(number);
    }

    private static boolean optionalBoolean(JsonObject value, String key, boolean fallback, String source) {
        JsonElement member = value.get(key);
        if (member == null || member.isJsonNull())
            return fallback;
        if (!member.isJsonPrimitive() || !member.getAsJsonPrimitive().isBoolean())
            throw new IllegalArgumentException("Expected boolean " + key + " at " + source);
        return member.getAsBoolean();
    }

    private static void rejectUnknown(JsonObject value, Set<String> allowed, String source) {
        for (Map.Entry<String, JsonElement> entry : value.entrySet())
            if (!allowed.contains(entry.getKey()))
                throw new IllegalArgumentException("Unknown field " + entry.getKey() + " at " + source);
    }

    private static Set<String> fields(String... values) {
        return Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(values)));
    }

    private static int[] concat(int[] left, int[] right) {
        int[] result = Arrays.copyOf(left, left.length + right.length);
        System.arraycopy(right, 0, result, left.length, right.length);
        return result;
    }

    private static String pointer(String source, String key, int index) {
        return source + (source.indexOf('#') >= 0 ? "/" : "#/") + key + "/" + index;
    }

    private static String relative(Path root, Path file) {
        return root.relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    private static String message(Throwable failure) {
        String value = failure.getMessage();
        return value == null || value.trim().isEmpty() ? failure.getClass().getSimpleName() : value;
    }

    private static void record(String code, PonderDiagnosticSeverity severity, String message,
                               ResourceLocation sceneId, int instructionIndex) {
        if (severity == PonderDiagnosticSeverity.ERROR)
            Ponder.LOGGER.error("Ponder JSON: {}", message);
        else
            Ponder.LOGGER.warn("Ponder JSON: {}", message);
        ScriptSceneRegistry.recordJsonIssue(code, severity, message, sceneId, instructionIndex);
    }

    public static final class ReloadResult {
        public final int packs;
        public final int scenes;
        public final int warnings;
        public final int errors;

        ReloadResult(int packs, int scenes, int warnings, int errors) {
            this.packs = packs;
            this.scenes = scenes;
            this.warnings = warnings;
            this.errors = errors;
        }
    }

    private static final class InstructionSpec {
        final String internalOperation;
        final Set<String> fields;

        InstructionSpec(String internalOperation, Set<String> fields) {
            this.internalOperation = internalOperation;
            this.fields = fields;
        }
    }

    private static final class Pack {
        final ResourceLocation id;
        final String source;
        final List<ScriptSceneDefinition> scenes;
        final List<ScriptTagDefinition> tags;
        final Map<String, String> sharedText;
        final List<ResourceLocation> indexExclusions;

        Pack(ResourceLocation id, String source, List<ScriptSceneDefinition> scenes,
             List<ScriptTagDefinition> tags, Map<String, String> sharedText,
             List<ResourceLocation> indexExclusions) {
            this.id = id;
            this.source = source;
            this.scenes = scenes;
            this.tags = tags;
            this.sharedText = sharedText;
            this.indexExclusions = indexExclusions;
        }
    }

    private static final class Snapshot {
        final List<ScriptSceneDefinition> scenes;
        final List<ScriptTagDefinition> tags;
        final Map<String, String> sharedText;
        final List<ResourceLocation> indexExclusions;

        Snapshot(List<ScriptSceneDefinition> scenes, List<ScriptTagDefinition> tags,
                 Map<String, String> sharedText, List<ResourceLocation> indexExclusions) {
            this.scenes = scenes;
            this.tags = tags;
            this.sharedText = sharedText;
            this.indexExclusions = indexExclusions;
        }
    }

    private static final class SnapshotBuilder {
        final Set<ResourceLocation> packIds = new HashSet<ResourceLocation>();
        final Map<ResourceLocation, ScriptSceneDefinition> scenes =
            new LinkedHashMap<ResourceLocation, ScriptSceneDefinition>();
        final Map<ResourceLocation, ScriptTagDefinition> tags =
            new LinkedHashMap<ResourceLocation, ScriptTagDefinition>();
        final Map<String, String> sharedText = new LinkedHashMap<String, String>();
        final List<ResourceLocation> indexExclusions = new ArrayList<ResourceLocation>();

        void add(Pack pack) {
            if (packIds.contains(pack.id))
                throw new IllegalArgumentException("Duplicate Ponder JSON pack id " + pack.id);
            for (ScriptSceneDefinition scene : pack.scenes) {
                if (ScriptSceneRegistry.containsZenScene(scene.getSceneId()))
                    throw new IllegalArgumentException(
                        "JSON scene conflicts with ZenScript scene " + scene.getSceneId());
                if (scenes.containsKey(scene.getSceneId()))
                    throw new IllegalArgumentException("Duplicate JSON scene id " + scene.getSceneId());
            }
            for (ScriptTagDefinition tag : pack.tags) {
                if (ScriptTagRegistry.containsZenTag(tag.id))
                    throw new IllegalArgumentException("JSON tag conflicts with ZenScript tag " + tag.id);
                if (tags.containsKey(tag.id))
                    throw new IllegalArgumentException("Duplicate JSON tag id " + tag.id);
            }
            for (Map.Entry<String, String> entry : pack.sharedText.entrySet()) {
                if (ScriptSharedText.containsZenKey(entry.getKey()))
                    throw new IllegalArgumentException(
                        "JSON shared text conflicts with ZenScript key " + entry.getKey());
                if (sharedText.containsKey(entry.getKey()))
                    throw new IllegalArgumentException("Duplicate JSON shared text key " + entry.getKey());
            }
            packIds.add(pack.id);
            for (ScriptSceneDefinition scene : pack.scenes)
                scenes.put(scene.getSceneId(), scene);
            for (ScriptTagDefinition tag : pack.tags)
                tags.put(tag.id, tag);
            sharedText.putAll(pack.sharedText);
            for (ResourceLocation exclusion : pack.indexExclusions)
                if (!indexExclusions.contains(exclusion)) indexExclusions.add(exclusion);
        }

        Snapshot build() {
            return new Snapshot(
                new ArrayList<ScriptSceneDefinition>(scenes.values()),
                new ArrayList<ScriptTagDefinition>(tags.values()),
                new LinkedHashMap<String, String>(sharedText),
                new ArrayList<ResourceLocation>(indexExclusions));
        }
    }

    private static final class ScanResult {
        final Snapshot snapshot;
        final Map<String, Pack> selected;
        final int warnings;
        final int errors;

        ScanResult(Snapshot snapshot, Map<String, Pack> selected, int warnings, int errors) {
            this.snapshot = snapshot;
            this.selected = selected;
            this.warnings = warnings;
            this.errors = errors;
        }
    }
}
