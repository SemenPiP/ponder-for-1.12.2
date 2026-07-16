package net.createmod.ponder.mmce.script;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import crafttweaker.annotations.ZenRegister;
import net.createmod.ponder.mmce.PonderMMCE;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
import stanhebben.zenscript.annotations.ZenProperty;

@ZenRegister
@ZenClass("mods.ponder.mmce.MMCEStructureRef")
public final class MMCEStructureRef {
    private static final String PATH_PREFIX = "structure";
    private static final String UNRESOLVED_FINGERPRINT =
        "0000000000000000000000000000000000000000000000000000000000000000";

    @ZenProperty public final String id;
    @ZenProperty public final String structure;
    @ZenProperty public final String component;
    @ZenProperty public final String machineId;
    @ZenProperty public final boolean dynamic;
    @ZenProperty public final String dynamicPattern;
    @ZenProperty public final int repetitions;
    @ZenProperty public final String patternOffset;
    @ZenProperty public final String facing;
    @ZenProperty public final boolean includePreviewNbt;
    @ZenProperty public final String fingerprint;
    @ZenProperty public final int sizeX;
    @ZenProperty public final int sizeY;
    @ZenProperty public final int sizeZ;
    @ZenProperty public final int controllerX;
    @ZenProperty public final int controllerY;
    @ZenProperty public final int controllerZ;
    @ZenProperty public final int basePlateSize;

    private final ResourceLocation resourceId;

    private MMCEStructureRef(ResourceLocation machineId, boolean dynamic, String dynamicPattern,
                             int repetitions, EnumFacing patternOffset, EnumFacing facing,
                             boolean includePreviewNbt, String fingerprint,
                             int sizeX, int sizeY, int sizeZ,
                             int controllerX, int controllerY, int controllerZ) {
        this.machineId = machineId.toString();
        this.dynamic = dynamic;
        this.dynamicPattern = dynamic ? requireText(dynamicPattern, "dynamic pattern") : "";
        this.repetitions = dynamic ? requireRepetitions(repetitions) : 0;
        this.patternOffset = dynamic ? horizontalName(patternOffset, "pattern offset") : "";
        this.facing = dynamic ? horizontalName(facing, "facing") : "";
        this.includePreviewNbt = includePreviewNbt;
        this.fingerprint = requireFingerprint(fingerprint);
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.controllerX = controllerX;
        this.controllerY = controllerY;
        this.controllerZ = controllerZ;
        this.basePlateSize = Math.max(1, Math.max(sizeX, sizeZ));
        this.resourceId = new ResourceLocation(PonderMMCE.MOD_ID, buildPath());
        if (resourceId.toString().length() > 256)
            throw new IllegalArgumentException("Ponder-MMCE structure ID exceeds 256 characters");
        this.id = resourceId.toString();
        this.structure = id;
        this.component = componentId(machineId).toString();
    }

    public static MMCEStructureRef unresolvedStatic(String machineId, boolean includePreviewNbt) {
        return new MMCEStructureRef(parseMachineId(machineId), false, "", 0,
            EnumFacing.NORTH, EnumFacing.NORTH, includePreviewNbt, UNRESOLVED_FINGERPRINT,
            -1, -1, -1, -1, -1, -1);
    }

    public static MMCEStructureRef unresolvedDynamic(String machineId, String dynamicPattern, int repetitions,
                                                     String patternOffset, String facing,
                                                     boolean includePreviewNbt) {
        return new MMCEStructureRef(parseMachineId(machineId), true, dynamicPattern, repetitions,
            parseHorizontalFacing(patternOffset, "pattern offset"),
            parseHorizontalFacing(facing, "facing"), includePreviewNbt, UNRESOLVED_FINGERPRINT,
            -1, -1, -1, -1, -1, -1);
    }

    public MMCEStructureRef resolved(String contentFingerprint, int width, int height, int depth,
                                     int normalizedControllerX, int normalizedControllerY,
                                     int normalizedControllerZ) {
        return new MMCEStructureRef(getMachineResourceLocation(), dynamic, dynamicPattern, repetitions,
            getPatternOffsetValue(), getFacingValue(), includePreviewNbt, contentFingerprint,
            width, height, depth, normalizedControllerX, normalizedControllerY, normalizedControllerZ);
    }

    public static MMCEStructureRef tryParse(ResourceLocation id) {
        if (id == null || !PonderMMCE.MOD_ID.equals(id.getNamespace())) return null;
        String[] parts = id.getPath().split("/", -1);
        try {
            if (parts.length == 5 && PATH_PREFIX.equals(parts[0]) && "static".equals(parts[1])) {
                return parsed(id, decode(parts[2]), false, "", 0, "", "",
                    parsePreview(parts[3]), parts[4]);
            }
            if (parts.length == 9 && PATH_PREFIX.equals(parts[0]) && "dynamic".equals(parts[1])) {
                return parsed(id, decode(parts[2]), true, decode(parts[3]),
                    Integer.parseInt(parts[4]), parts[5], parts[6],
                    parsePreview(parts[7]), parts[8]);
            }
            return null;
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }

    private static MMCEStructureRef parsed(ResourceLocation expectedId, String machineId, boolean dynamic,
                                            String dynamicPattern, int repetitions, String patternOffset,
                                            String facing, boolean preview, String fingerprint) {
        MMCEStructureRef parsed = dynamic
            ? new MMCEStructureRef(parseMachineId(machineId), true, dynamicPattern, repetitions,
                parseHorizontalFacing(patternOffset, "pattern offset"),
                parseHorizontalFacing(facing, "facing"), preview, fingerprint,
                -1, -1, -1, -1, -1, -1)
            : new MMCEStructureRef(parseMachineId(machineId), false, "", 0,
                EnumFacing.NORTH, EnumFacing.NORTH, preview, fingerprint,
                -1, -1, -1, -1, -1, -1);
        return parsed.resourceId.equals(expectedId) ? parsed : null;
    }

    @ZenMethod
    public String asString() {
        return id;
    }

    public ResourceLocation asResourceLocation() {
        return resourceId;
    }

    public ResourceLocation getMachineResourceLocation() {
        return new ResourceLocation(machineId);
    }

    public EnumFacing getPatternOffsetValue() {
        return dynamic ? EnumFacing.byName(patternOffset) : EnumFacing.NORTH;
    }

    public EnumFacing getFacingValue() {
        return dynamic ? EnumFacing.byName(facing) : EnumFacing.NORTH;
    }

    public String canonicalSpec() {
        return "ponder-mmce-ref-v2\0" + (dynamic ? "dynamic" : "static") + "\0" + machineId + "\0"
            + dynamicPattern + "\0" + repetitions + "\0" + patternOffset + "\0" + facing + "\0"
            + includePreviewNbt;
    }

    public static ResourceLocation componentId(ResourceLocation machineId) {
        if (machineId == null) throw new IllegalArgumentException("machine id is required");
        return new ResourceLocation(PonderMMCE.MOD_ID,
            "machine/" + machineId.getNamespace() + "/" + machineId.getPath());
    }

    private String buildPath() {
        String preview = "preview-" + (includePreviewNbt ? "1" : "0");
        if (!dynamic)
            return PATH_PREFIX + "/static/" + encode(machineId) + "/" + preview + "/" + fingerprint;
        return PATH_PREFIX + "/dynamic/" + encode(machineId) + "/" + encode(dynamicPattern) + "/"
            + repetitions + "/" + patternOffset + "/" + facing + "/" + preview + "/" + fingerprint;
    }

    private static ResourceLocation parseMachineId(String value) {
        String normalized = requireText(value, "machine id");
        return normalized.indexOf(':') >= 0
            ? new ResourceLocation(normalized)
            : new ResourceLocation("modularmachinery", normalized);
    }

    private static EnumFacing parseHorizontalFacing(String value, String label) {
        EnumFacing parsed = EnumFacing.byName(requireText(value, label).toLowerCase(Locale.ROOT));
        if (parsed == null || parsed.getAxis().isVertical())
            throw new IllegalArgumentException(label + " must be north, east, south, or west");
        return parsed;
    }

    private static String horizontalName(EnumFacing value, String label) {
        if (value == null || value.getAxis().isVertical())
            throw new IllegalArgumentException(label + " must be horizontal");
        return value.getName();
    }

    private static int requireRepetitions(int value) {
        if (value < 0) throw new IllegalArgumentException("dynamic repetitions must be zero or greater");
        return value;
    }

    private static String requireText(String value, String label) {
        if (value == null || value.trim().isEmpty())
            throw new IllegalArgumentException(label + " is required");
        return value.trim();
    }

    private static String requireFingerprint(String value) {
        String normalized = requireText(value, "structure fingerprint").toLowerCase(Locale.ROOT);
        if (!normalized.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("structure fingerprint must be a SHA-256 value");
        return normalized;
    }

    private static boolean parsePreview(String value) {
        if ("preview-1".equals(value)) return true;
        if ("preview-0".equals(value)) return false;
        throw new IllegalArgumentException("invalid preview flag");
    }

    private static String encode(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte valueByte : bytes)
            result.append(String.format(Locale.ROOT, "%02x", valueByte & 0xff));
        return result.toString();
    }

    private static String decode(String value) {
        if ((value.length() & 1) != 0)
            throw new IllegalArgumentException("invalid hexadecimal text");
        byte[] bytes = new byte[value.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int high = Character.digit(value.charAt(i * 2), 16);
            int low = Character.digit(value.charAt(i * 2 + 1), 16);
            if (high < 0 || low < 0)
                throw new IllegalArgumentException("invalid hexadecimal text");
            bytes[i] = (byte) ((high << 4) | low);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
