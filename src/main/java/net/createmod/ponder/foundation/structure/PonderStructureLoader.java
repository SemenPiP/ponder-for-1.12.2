package net.createmod.ponder.foundation.structure;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import net.createmod.ponder.api.structure.PonderStructureProvider;
import net.createmod.ponder.api.structure.PonderStructureProviderResult;
import net.createmod.ponder.api.structure.PonderStructureProviders;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class PonderStructureLoader {
    public interface ResourceProvider {
        InputStream open(ResourceLocation location) throws IOException;
    }

    private static final int MAX_PALETTE = 65536;
    private static final int MAX_BLOCKS = 16 * 1024 * 1024;
    private static final int MAX_EXTERNAL_CACHE_ENTRIES = 64;
    private static final int MAX_PARSED_CACHE_ENTRIES = 128;
    private static final long MAX_STRUCTURE_BYTES = 16L * 1024L * 1024L;
    private static volatile ResourceProvider resourceProvider;
    private static volatile File externalRoot;
    private static final Map<String, CachedExternalResult> EXTERNAL_CACHE =
        new LinkedHashMap<String, CachedExternalResult>(16, .75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CachedExternalResult> eldest) {
                return size() > MAX_EXTERNAL_CACHE_ENTRIES;
            }
        };
    private static final Map<ParsedCacheKey, PonderStructure> PARSED_CACHE =
        new LinkedHashMap<ParsedCacheKey, PonderStructure>(16, .75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<ParsedCacheKey, PonderStructure> eldest) {
                return size() > MAX_PARSED_CACHE_ENTRIES;
            }
        };
    private final LegacyStateResolver resolver;

    public PonderStructureLoader() {
        this(new LegacyStateResolver());
    }

    public PonderStructureLoader(LegacyStateResolver resolver) {
        if (resolver == null)
            throw new IllegalArgumentException("Legacy state resolver is required");
        this.resolver = resolver;
    }

    public static void setResourceProvider(ResourceProvider provider) {
        resourceProvider = provider;
    }

    public static synchronized void setExternalRoot(File root) {
        externalRoot = root;
        EXTERNAL_CACHE.clear();
        PARSED_CACHE.clear();
    }

    public static void invalidateCaches() {
        synchronized (PonderStructureLoader.class) {
            EXTERNAL_CACHE.clear();
            PARSED_CACHE.clear();
        }
        PonderStructureProviders.invalidate();
    }

    public static String expectedExternalPath(ResourceLocation id) {
        File configuredRoot = externalRoot;
        if (configuredRoot == null)
            return "scripts/ponder/structures/" + id.getNamespace() + "/" + id.getPath() + ".nbt";
        return configuredRoot.toPath().toAbsolutePath().normalize()
            .resolve(id.getNamespace()).resolve(id.getPath() + ".nbt").normalize().toString();
    }

    public PonderStructure load(ResourceLocation id) throws IOException {
        if (id == null)
            throw new IllegalArgumentException("Ponder structure ID is required");
        ResolvedSource source = resolve(id);
        return parseResult(source.providerId, source.result, id);
    }

    public PonderStructureProviderResult loadResult(ResourceLocation id) throws IOException {
        if (id == null)
            throw new IllegalArgumentException("Ponder structure ID is required");
        ResolvedSource source = resolve(id);
        PonderStructure structure = parseResult(source.providerId, source.result, id);
        return PonderStructureProviderResult.found(source.result.getNbtBytes(),
            source.result.getFingerprint(), structure.getGroups(), structure.getDiagnostics());
    }

    private ResolvedSource resolve(ResourceLocation id) throws IOException {
        List<String> misses = new ArrayList<String>();

        PonderStructureProviderResult external = loadExternal(id);
        if (external.isFound())
            return new ResolvedSource(PonderStructureProviders.EXTERNAL_FILE_ID,
                withDiagnostics(external, misses));
        appendDiagnostics(misses, PonderStructureProviders.EXTERNAL_FILE_ID, external.getDiagnostics());

        for (PonderStructureProvider provider : PonderStructureProviders.snapshot()) {
            PonderStructureProviderResult result;
            try {
                result = provider.find(id);
            } catch (IOException exception) {
                throw providerFailure(provider.getId(), id, exception);
            } catch (RuntimeException exception) {
                throw providerFailure(provider.getId(), id, exception);
            }
            if (result == null)
                throw new IOException("Ponder structure provider " + provider.getId()
                    + " returned null for " + id);
            if (result.isFound())
                return new ResolvedSource(provider.getId(), withDiagnostics(result, misses));
            appendDiagnostics(misses, provider.getId(), result.getDiagnostics());
        }

        PonderStructureProviderResult resource = loadResourcePack(id);
        if (resource.isFound())
            return new ResolvedSource(PonderStructureProviders.RESOURCE_PACK_ID,
                withDiagnostics(resource, misses));
        appendDiagnostics(misses, PonderStructureProviders.RESOURCE_PACK_ID, resource.getDiagnostics());

        PonderStructureProviderResult jar = loadJar(id);
        if (jar.isFound())
            return new ResolvedSource(PonderStructureProviders.JAR_ID, withDiagnostics(jar, misses));
        appendDiagnostics(misses, PonderStructureProviders.JAR_ID, jar.getDiagnostics());

        FileNotFoundException missing = new FileNotFoundException(assetLocation(id).toString()
            + (misses.isEmpty() ? "" : "; diagnostics: " + misses));
        throw missing;
    }

    private PonderStructureProviderResult loadExternal(ResourceLocation id) throws IOException {
        File configuredRoot = externalRoot;
        if (configuredRoot == null)
            return PonderStructureProviderResult.notFound();
        Path root = configuredRoot.toPath().toAbsolutePath().normalize();
        Path candidate = root.resolve(id.getNamespace()).resolve(id.getPath() + ".nbt").normalize();
        if (!candidate.startsWith(root))
            throw new IOException("Ponder structure path escapes scripts root: " + id);
        rejectLinkedSegments(root, id);
        if (!Files.exists(candidate, LinkOption.NOFOLLOW_LINKS))
            return PonderStructureProviderResult.notFound();
        rejectLinkedSegments(candidate, id);
        if (!Files.isRegularFile(candidate, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(candidate))
            throw new IOException("Ponder structure is not a regular NBT file: " + candidate);
        Path realRoot = Files.exists(root, LinkOption.NOFOLLOW_LINKS) ? root.toRealPath() : root;
        Path realCandidate = candidate.toRealPath();
        if (!realCandidate.startsWith(realRoot))
            throw new IOException("Ponder structure resolves outside scripts root: " + id);
        long length = Files.size(realCandidate);
        if (length > MAX_STRUCTURE_BYTES)
            throw new IOException("Ponder structure exceeds " + MAX_STRUCTURE_BYTES + " bytes: " + id);
        if (length == 0)
            throw new IOException("Empty structure NBT: " + id);
        long modified = Files.getLastModifiedTime(realCandidate).toMillis();
        String key = realCandidate.toString();
        byte[] bytes = Files.readAllBytes(realCandidate);
        String hash = sha256(bytes);
        synchronized (PonderStructureLoader.class) {
            CachedExternalResult cached = EXTERNAL_CACHE.get(key);
            if (cached != null && cached.modified == modified && cached.length == length
                && cached.hash.equals(hash))
                return cached.result;
        }
        PonderStructureProviderResult result = PonderStructureProviderResult.found(bytes, hash);
        synchronized (PonderStructureLoader.class) {
            EXTERNAL_CACHE.put(key, new CachedExternalResult(modified, length, hash, result));
        }
        return result;
    }

    private PonderStructureProviderResult loadResourcePack(ResourceLocation id) throws IOException {
        ResourceProvider configured = resourceProvider;
        if (configured == null)
            return PonderStructureProviderResult.notFound();
        ResourceLocation asset = assetLocation(id);
        InputStream stream;
        try {
            stream = configured.open(asset);
        } catch (FileNotFoundException missing) {
            return PonderStructureProviderResult.notFound();
        } catch (IOException exception) {
            throw new IOException("Ponder resource provider failed for " + id + ": "
                + exception.getMessage(), exception);
        }
        if (stream == null)
            return PonderStructureProviderResult.notFound();
        try (InputStream closeable = stream) {
            byte[] bytes = readBytes(closeable, id);
            return PonderStructureProviderResult.found(bytes, sha256(bytes));
        }
    }

    private PonderStructureProviderResult loadJar(ResourceLocation id) throws IOException {
        ResourceLocation asset = assetLocation(id);
        String path = "assets/" + asset.getNamespace() + "/" + asset.getPath();
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        InputStream stream = context == null ? null : context.getResourceAsStream(path);
        if (stream == null)
            stream = PonderStructureLoader.class.getClassLoader().getResourceAsStream(path);
        if (stream == null)
            return PonderStructureProviderResult.notFound();
        try (InputStream closeable = stream) {
            byte[] bytes = readBytes(closeable, id);
            return PonderStructureProviderResult.found(bytes, sha256(bytes));
        }
    }

    private static ResourceLocation assetLocation(ResourceLocation id) {
        return new ResourceLocation(id.getNamespace(), "ponder/" + id.getPath() + ".nbt");
    }

    private static void rejectLinkedSegments(Path path, ResourceLocation id) throws IOException {
        Path absolute = path.toAbsolutePath().normalize();
        Path current = absolute.getRoot();
        if (current == null)
            throw new IOException("Ponder structure path has no filesystem root: " + absolute);
        for (Path segment : absolute) {
            current = current.resolve(segment);
            if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS))
                continue;
            if (Files.isSymbolicLink(current))
                throw new IOException("Ponder structure path contains a symbolic link: " + id + " at " + current);
            Path noFollow = current.toRealPath(LinkOption.NOFOLLOW_LINKS);
            Path followed = current.toRealPath();
            if (!noFollow.equals(followed))
                throw new IOException("Ponder structure path contains a linked directory or junction: "
                    + id + " at " + current);
        }
    }

    private static byte[] readBytes(InputStream input, ResourceLocation id) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;
        while ((read = input.read(buffer)) >= 0) {
            if (read == 0)
                continue;
            total += read;
            if (total > MAX_STRUCTURE_BYTES)
                throw new IOException("Ponder structure exceeds " + MAX_STRUCTURE_BYTES + " bytes: " + id);
            output.write(buffer, 0, read);
        }
        if (total == 0)
            throw new IOException("Empty structure NBT: " + id);
        return output.toByteArray();
    }

    private static String sha256(byte[] bytes) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder result = new StringBuilder();
            for (byte value : digest.digest(bytes))
                result.append(String.format("%02x", value & 0xff));
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IOException("SHA-256 is unavailable", impossible);
        }
    }

    public PonderStructure load(InputStream input, ResourceLocation source) throws IOException {
        if (input == null)
            throw new IllegalArgumentException("Ponder structure input is required");
        byte[] bytes = readBytes(input, source);
        PonderStructureProviderResult result = PonderStructureProviderResult.found(bytes, sha256(bytes));
        return parseResult(PonderStructureProviders.DIRECT_ID, result, source);
    }

    private PonderStructure parseResult(ResourceLocation providerId, PonderStructureProviderResult result,
                                        ResourceLocation source) throws IOException {
        ParsedCacheKey cacheKey = new ParsedCacheKey(providerId, source, result.getFingerprint(),
            result.getGroups(), result.getDiagnostics());
        synchronized (PonderStructureLoader.class) {
            PonderStructure cached = PARSED_CACHE.get(cacheKey);
            if (cached != null)
                return cached;
        }
        byte[] bytes = result.getNbtBytes();
        if (bytes == null)
            throw new IOException("Found Ponder structure result has no NBT bytes: " + source);
        NBTTagCompound root = readRoot(new ByteArrayInputStream(bytes), source);
        PonderStructure parsed = parse(root, source, providerId, result.getFingerprint(), result.getGroups(),
            result.getDiagnostics());
        synchronized (PonderStructureLoader.class) {
            PARSED_CACHE.put(cacheKey, parsed);
        }
        return parsed;
    }

    private static NBTTagCompound readRoot(InputStream input, ResourceLocation source) throws IOException {
        BufferedInputStream buffered = new BufferedInputStream(input);
        buffered.mark(2);
        int first = buffered.read();
        int second = buffered.read();
        buffered.reset();
        NBTTagCompound root = first == 0x1f && second == 0x8b
            ? CompressedStreamTools.readCompressed(buffered)
            : CompressedStreamTools.read(new DataInputStream(buffered));
        if (root == null)
            throw new IOException("Empty structure NBT: " + source);
        return root;
    }

    public PonderStructure parse(NBTTagCompound root, ResourceLocation source) throws IOException {
        return parse(root, source, PonderStructureProviders.DIRECT_ID, "direct:" + source,
            Collections.<String, List<BlockPos>>emptyMap(), Collections.<String>emptyList());
    }

    private PonderStructure parse(NBTTagCompound root, ResourceLocation source, ResourceLocation providerId,
                                  String fingerprint, Map<String, List<BlockPos>> suppliedGroups,
                                  List<String> suppliedDiagnostics) throws IOException {
        int[] sizeArray = readIntVector(root, "size");
        if (sizeArray[0] <= 0 || sizeArray[1] <= 0 || sizeArray[2] <= 0)
            throw malformed(source, "size must contain three positive values");
        long volume = (long) sizeArray[0] * sizeArray[1] * sizeArray[2];
        if (volume > MAX_BLOCKS)
            throw malformed(source, "structure volume exceeds " + MAX_BLOCKS);
        BlockPos size = new BlockPos(sizeArray[0], sizeArray[1], sizeArray[2]);
        NBTTagList palette = root.getTagList("palette", 10);
        if (palette.tagCount() == 0 && root.hasKey("palettes", 9)) {
            NBTTagList palettes = root.getTagList("palettes", 9);
            if (palettes.tagCount() > 0 && palettes.get(0) instanceof NBTTagList)
                palette = (NBTTagList) palettes.get(0);
        }
        if (palette.tagCount() == 0 || palette.tagCount() > MAX_PALETTE)
            throw malformed(source, "palette is empty or too large");
        List<LegacyStateResolver.Resolution> states =
            new ArrayList<LegacyStateResolver.Resolution>(palette.tagCount());
        List<String> diagnostics = new ArrayList<String>(suppliedDiagnostics);
        for (int i = 0; i < palette.tagCount(); i++) {
            LegacyStateResolver.Resolution resolution = resolver.resolve(palette.getCompoundTagAt(i));
            states.add(resolution);
            if (resolution.getDiagnostic() != null)
                diagnostics.add("palette[" + i + "]: " + resolution.getDiagnostic());
        }

        Map<String, LinkedHashSet<BlockPos>> groups =
            new LinkedHashMap<String, LinkedHashSet<BlockPos>>();
        NBTTagList blockList = root.getTagList("blocks", 10);
        if (blockList.tagCount() > MAX_BLOCKS)
            throw malformed(source, "block list exceeds " + MAX_BLOCKS);
        Map<BlockPos, PonderStructure.BlockInfo> blocks =
            new LinkedHashMap<BlockPos, PonderStructure.BlockInfo>();
        for (int i = 0; i < blockList.tagCount(); i++) {
            NBTTagCompound entry = blockList.getCompoundTagAt(i);
            int[] posArray = readIntVector(entry, "pos");
            BlockPos pos = new BlockPos(posArray[0], posArray[1], posArray[2]);
            if (!isInside(pos, size)) {
                diagnostics.add("blocks[" + i + "] is outside declared size: " + pos);
                continue;
            }
            int stateIndex = entry.getInteger("state");
            if (stateIndex < 0 || stateIndex >= states.size()) {
                diagnostics.add("blocks[" + i + "] references invalid palette index "
                    + stateIndex + "; barrier used");
                stateIndex = -1;
            }
            NBTTagCompound tile = entry.hasKey("nbt", 10) ? entry.getCompoundTag("nbt") : null;
            PonderStructure.BlockInfo previous = blocks.put(pos, new PonderStructure.BlockInfo(pos,
                stateIndex < 0 ? net.minecraft.init.Blocks.BARRIER.getDefaultState()
                    : states.get(stateIndex).getState(), tile));
            if (previous != null)
                diagnostics.add("duplicate block position " + pos + "; last entry retained");
            readBlockGroups(entry, pos, groups, diagnostics, i);
        }

        readTopLevelGroups(root, size, groups, diagnostics);
        for (Map.Entry<String, List<BlockPos>> group : suppliedGroups.entrySet()) {
            ensureGroup(groups, group.getKey());
            for (BlockPos position : group.getValue()) {
                if (isInside(position, size))
                    groups.get(group.getKey()).add(position.toImmutable());
                else
                    diagnostics.add("provider group " + group.getKey()
                        + " contains position outside declared size: " + position);
            }
        }

        List<PonderStructure.EntityInfo> entities = new ArrayList<PonderStructure.EntityInfo>();
        NBTTagList entityList = root.getTagList("entities", 10);
        for (int i = 0; i < entityList.tagCount(); i++) {
            NBTTagCompound entry = entityList.getCompoundTagAt(i);
            if (!entry.hasKey("nbt", 10)) {
                diagnostics.add("entities[" + i + "] has no nbt payload");
                continue;
            }
            double[] pos = readDoubleVector(entry, "pos");
            entities.add(new PonderStructure.EntityInfo(new Vec3d(pos[0], pos[1], pos[2]),
                entry.getCompoundTag("nbt")));
        }
        return new PonderStructure(size, new ArrayList<PonderStructure.BlockInfo>(blocks.values()), entities,
            providerId, fingerprint, immutableGroups(groups), diagnostics);
    }

    private static void readBlockGroups(NBTTagCompound entry, BlockPos position,
                                        Map<String, LinkedHashSet<BlockPos>> groups,
                                        List<String> diagnostics, int blockIndex) {
        if (!entry.hasKey("ponder_groups", 9))
            return;
        NBTTagList names = entry.getTagList("ponder_groups", 8);
        if (names.tagCount() == 0) {
            NBTTagList raw = (NBTTagList) entry.getTag("ponder_groups");
            if (raw.tagCount() > 0 && raw.getTagType() != 8) {
                diagnostics.add("blocks[" + blockIndex
                    + "].ponder_groups must be a list of strings");
                return;
            }
        }
        for (int i = 0; i < names.tagCount(); i++) {
            String name = names.getStringTagAt(i);
            if (!isValidGroupName(name)) {
                diagnostics.add("blocks[" + blockIndex + "] has an invalid Ponder group name");
                continue;
            }
            ensureGroup(groups, name).add(position.toImmutable());
        }
    }

    private static void readTopLevelGroups(NBTTagCompound root, BlockPos size,
                                           Map<String, LinkedHashSet<BlockPos>> groups,
                                           List<String> diagnostics) {
        if (!root.hasKey("ponder_groups", 9))
            return;
        NBTTagList groupList = root.getTagList("ponder_groups", 10);
        if (groupList.tagCount() == 0) {
            NBTTagList raw = (NBTTagList) root.getTag("ponder_groups");
            if (raw.tagCount() > 0 && raw.getTagType() != 10) {
                diagnostics.add("ponder_groups must be a list of compounds");
                return;
            }
        }
        for (int i = 0; i < groupList.tagCount(); i++) {
            NBTTagCompound group = groupList.getCompoundTagAt(i);
            String name = group.getString("name");
            if (!isValidGroupName(name)) {
                diagnostics.add("ponder_groups[" + i + "] has an invalid name");
                continue;
            }
            LinkedHashSet<BlockPos> positions = ensureGroup(groups, name);
            if (!group.hasKey("positions", 9)) {
                diagnostics.add("ponder_groups[" + i + "] has no positions list");
                continue;
            }
            NBTTagList positionList = group.getTagList("positions", 9);
            if (positionList.tagCount() == 0) {
                NBTTagList raw = (NBTTagList) group.getTag("positions");
                if (raw.tagCount() > 0 && raw.getTagType() != 9) {
                    diagnostics.add("ponder_groups[" + i + "].positions must be a list of integer vectors");
                    continue;
                }
            }
            for (int j = 0; j < positionList.tagCount(); j++) {
                NBTBase value = positionList.get(j);
                if (!(value instanceof NBTTagList)) {
                    diagnostics.add("ponder_groups[" + i + "].positions[" + j
                        + "] is not an integer vector");
                    continue;
                }
                NBTTagList vector = (NBTTagList) value;
                if (vector.tagCount() != 3) {
                    diagnostics.add("ponder_groups[" + i + "].positions[" + j
                        + "] must contain three integers");
                    continue;
                }
                BlockPos position = new BlockPos(vector.getIntAt(0), vector.getIntAt(1), vector.getIntAt(2));
                if (!isInside(position, size)) {
                    diagnostics.add("ponder_groups[" + i + "].positions[" + j
                        + "] is outside declared size: " + position);
                    continue;
                }
                positions.add(position);
            }
        }
    }

    private static LinkedHashSet<BlockPos> ensureGroup(Map<String, LinkedHashSet<BlockPos>> groups,
                                                        String name) {
        LinkedHashSet<BlockPos> positions = groups.get(name);
        if (positions == null) {
            positions = new LinkedHashSet<BlockPos>();
            groups.put(name, positions);
        }
        return positions;
    }

    private static Map<String, List<BlockPos>> immutableGroups(
        Map<String, LinkedHashSet<BlockPos>> source) {
        if (source.isEmpty())
            return Collections.emptyMap();
        Map<String, List<BlockPos>> result = new LinkedHashMap<String, List<BlockPos>>();
        for (Map.Entry<String, LinkedHashSet<BlockPos>> entry : source.entrySet())
            result.put(entry.getKey(), Collections.unmodifiableList(
                new ArrayList<BlockPos>(entry.getValue())));
        return Collections.unmodifiableMap(result);
    }

    private static boolean isInside(BlockPos position, BlockPos size) {
        return position.getX() >= 0 && position.getY() >= 0 && position.getZ() >= 0
            && position.getX() < size.getX() && position.getY() < size.getY()
            && position.getZ() < size.getZ();
    }

    private static boolean isValidGroupName(String name) {
        return name != null && !name.trim().isEmpty() && name.length() <= 256;
    }

    private static int[] readIntVector(NBTTagCompound parent, String key) throws IOException {
        if (!parent.hasKey(key, 9))
            throw new IOException("Missing list '" + key + "'");
        NBTTagList list = parent.getTagList(key, 3);
        if (list.tagCount() != 3)
            throw new IOException("List '" + key + "' must have exactly 3 integers");
        return new int[] {list.getIntAt(0), list.getIntAt(1), list.getIntAt(2)};
    }

    private static double[] readDoubleVector(NBTTagCompound parent, String key) throws IOException {
        NBTTagList list = parent.getTagList(key, 6);
        if (list.tagCount() != 3)
            throw new IOException("List '" + key + "' must have exactly 3 doubles");
        return new double[] {list.getDoubleAt(0), list.getDoubleAt(1), list.getDoubleAt(2)};
    }

    private static PonderStructureProviderResult withDiagnostics(PonderStructureProviderResult result,
                                                                 List<String> previous) {
        if (previous.isEmpty())
            return result;
        List<String> diagnostics = new ArrayList<String>(previous);
        diagnostics.addAll(result.getDiagnostics());
        return PonderStructureProviderResult.found(result.getNbtBytes(), result.getFingerprint(),
            result.getGroups(), diagnostics);
    }

    private static void appendDiagnostics(List<String> target, ResourceLocation providerId,
                                          List<String> diagnostics) {
        for (String diagnostic : diagnostics)
            target.add(providerId + ": " + diagnostic);
    }

    private static IOException providerFailure(ResourceLocation providerId, ResourceLocation structureId,
                                               Exception cause) {
        return new IOException("Ponder structure provider " + providerId
            + " failed while loading " + structureId, cause);
    }

    private static IOException malformed(ResourceLocation source, String message) {
        return new IOException("Malformed Ponder structure " + source + ": " + message);
    }

    private static final class CachedExternalResult {
        final long modified;
        final long length;
        final String hash;
        final PonderStructureProviderResult result;

        CachedExternalResult(long modified, long length, String hash,
                             PonderStructureProviderResult result) {
            this.modified = modified;
            this.length = length;
            this.hash = hash;
            this.result = result;
        }
    }

    private static final class ResolvedSource {
        final ResourceLocation providerId;
        final PonderStructureProviderResult result;

        ResolvedSource(ResourceLocation providerId, PonderStructureProviderResult result) {
            this.providerId = providerId;
            this.result = result;
        }
    }

    private static final class ParsedCacheKey {
        final ResourceLocation providerId;
        final ResourceLocation structureId;
        final String fingerprint;
        final Map<String, List<BlockPos>> groups;
        final List<String> diagnostics;

        ParsedCacheKey(ResourceLocation providerId, ResourceLocation structureId, String fingerprint,
                       Map<String, List<BlockPos>> groups, List<String> diagnostics) {
            this.providerId = providerId;
            this.structureId = structureId;
            this.fingerprint = fingerprint;
            this.groups = groups;
            this.diagnostics = diagnostics;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other)
                return true;
            if (!(other instanceof ParsedCacheKey))
                return false;
            ParsedCacheKey key = (ParsedCacheKey) other;
            return providerId.equals(key.providerId) && structureId.equals(key.structureId)
                && fingerprint.equals(key.fingerprint) && groups.equals(key.groups)
                && diagnostics.equals(key.diagnostics);
        }

        @Override
        public int hashCode() {
            int result = providerId.hashCode();
            result = 31 * result + structureId.hashCode();
            result = 31 * result + fingerprint.hashCode();
            result = 31 * result + groups.hashCode();
            result = 31 * result + diagnostics.hashCode();
            return result;
        }
    }
}
