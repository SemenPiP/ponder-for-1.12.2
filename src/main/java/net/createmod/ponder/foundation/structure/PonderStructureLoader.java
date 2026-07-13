package net.createmod.ponder.foundation.structure;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.CompressedStreamTools;
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
    private static final long MAX_EXTERNAL_BYTES = 16L * 1024L * 1024L;
    private static volatile ResourceProvider resourceProvider;
    private static volatile File externalRoot;
    private static final Map<String, CachedExternalStructure> EXTERNAL_CACHE =
        new HashMap<String, CachedExternalStructure>();
    private final LegacyStateResolver resolver;

    public PonderStructureLoader() {
        this(new LegacyStateResolver());
    }

    public PonderStructureLoader(LegacyStateResolver resolver) {
        this.resolver = resolver;
    }

    public static void setResourceProvider(ResourceProvider provider) {
        resourceProvider = provider;
    }

    public static synchronized void setExternalRoot(File root) {
        externalRoot = root;
        EXTERNAL_CACHE.clear();
    }

    public static synchronized void invalidateCaches() {
        EXTERNAL_CACHE.clear();
    }

    public static String expectedExternalPath(ResourceLocation id) {
        File configuredRoot = externalRoot;
        if (configuredRoot == null)
            return "scripts/ponder/structures/" + id.getNamespace() + "/" + id.getPath() + ".nbt";
        return configuredRoot.toPath().toAbsolutePath().normalize()
            .resolve(id.getNamespace()).resolve(id.getPath() + ".nbt").normalize().toString();
    }

    public PonderStructure load(ResourceLocation id) throws IOException {
        ResourceLocation asset = new ResourceLocation(id.getNamespace(), "ponder/" + id.getPath() + ".nbt");
        PonderStructure external = loadExternal(id);
        if (external != null) return external;
        InputStream stream = null;
        if (resourceProvider != null) {
            try {
                stream = resourceProvider.open(asset);
            } catch (FileNotFoundException missing) {
                stream = null;
            }
        }
        if (stream == null) {
            String path = "assets/" + asset.getNamespace() + "/" + asset.getPath();
            ClassLoader context = Thread.currentThread().getContextClassLoader();
            stream = context == null ? null : context.getResourceAsStream(path);
            if (stream == null) stream = PonderStructureLoader.class.getClassLoader().getResourceAsStream(path);
        }
        if (stream == null) throw new FileNotFoundException(asset.toString());
        try (InputStream closeable = stream) {
            return load(closeable, id);
        }
    }

    private PonderStructure loadExternal(ResourceLocation id) throws IOException {
        File configuredRoot = externalRoot;
        if (configuredRoot == null) return null;
        Path root = configuredRoot.toPath().toAbsolutePath().normalize();
        Path candidate = root.resolve(id.getNamespace()).resolve(id.getPath() + ".nbt").normalize();
        if (!candidate.startsWith(root))
            throw new IOException("Ponder structure path escapes scripts root: " + id);
        rejectLinkedSegments(root, id);
        if (!Files.exists(candidate, LinkOption.NOFOLLOW_LINKS)) return null;
        rejectLinkedSegments(candidate, id);
        if (!Files.isRegularFile(candidate, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(candidate))
            throw new IOException("Ponder structure is not a regular NBT file: " + candidate);
        Path realRoot = Files.exists(root, LinkOption.NOFOLLOW_LINKS) ? root.toRealPath() : root;
        Path realCandidate = candidate.toRealPath();
        if (!realCandidate.startsWith(realRoot))
            throw new IOException("Ponder structure resolves outside scripts root: " + id);
        long length = Files.size(realCandidate);
        if (length > MAX_EXTERNAL_BYTES)
            throw new IOException("Ponder structure exceeds " + MAX_EXTERNAL_BYTES + " bytes: " + id);
        long modified = Files.getLastModifiedTime(realCandidate).toMillis();
        String key = realCandidate.toString();
        byte[] bytes = Files.readAllBytes(realCandidate);
        String hash = sha256(bytes);
        synchronized (PonderStructureLoader.class) {
            CachedExternalStructure cached = EXTERNAL_CACHE.get(key);
            if (cached != null && cached.modified == modified && cached.length == length && cached.hash.equals(hash))
                return cached.structure;
        }
        PonderStructure parsed = load(new java.io.ByteArrayInputStream(bytes), id);
        synchronized (PonderStructureLoader.class) {
            EXTERNAL_CACHE.put(key, new CachedExternalStructure(modified, length, hash, parsed));
        }
        return parsed;
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

    private static String sha256(byte[] bytes) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder result = new StringBuilder();
            for (byte value : digest.digest(bytes)) result.append(String.format("%02x", value & 0xff));
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IOException("SHA-256 is unavailable", impossible);
        }
    }

    private static final class CachedExternalStructure {
        final long modified;
        final long length;
        final String hash;
        final PonderStructure structure;

        CachedExternalStructure(long modified, long length, String hash, PonderStructure structure) {
            this.modified = modified; this.length = length; this.hash = hash; this.structure = structure;
        }
    }

    public PonderStructure load(InputStream input, ResourceLocation source) throws IOException {
        BufferedInputStream buffered = new BufferedInputStream(input);
        buffered.mark(2);
        int first = buffered.read();
        int second = buffered.read();
        buffered.reset();
        NBTTagCompound root = first == 0x1f && second == 0x8b
            ? CompressedStreamTools.readCompressed(buffered)
            : CompressedStreamTools.read(new DataInputStream(buffered));
        if (root == null) throw new IOException("Empty structure NBT: " + source);
        return parse(root, source);
    }

    public PonderStructure parse(NBTTagCompound root, ResourceLocation source) throws IOException {
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
        List<LegacyStateResolver.Resolution> states = new ArrayList<LegacyStateResolver.Resolution>(palette.tagCount());
        List<String> diagnostics = new ArrayList<String>();
        for (int i = 0; i < palette.tagCount(); i++) {
            LegacyStateResolver.Resolution resolution = resolver.resolve(palette.getCompoundTagAt(i));
            states.add(resolution);
            if (resolution.getDiagnostic() != null)
                diagnostics.add("palette[" + i + "]: " + resolution.getDiagnostic());
        }
        NBTTagList blockList = root.getTagList("blocks", 10);
        if (blockList.tagCount() > MAX_BLOCKS)
            throw malformed(source, "block list exceeds " + MAX_BLOCKS);
        Map<BlockPos, PonderStructure.BlockInfo> blocks = new LinkedHashMap<BlockPos, PonderStructure.BlockInfo>();
        for (int i = 0; i < blockList.tagCount(); i++) {
            NBTTagCompound entry = blockList.getCompoundTagAt(i);
            int[] posArray = readIntVector(entry, "pos");
            BlockPos pos = new BlockPos(posArray[0], posArray[1], posArray[2]);
            if (pos.getX() < 0 || pos.getY() < 0 || pos.getZ() < 0 || pos.getX() >= size.getX()
                || pos.getY() >= size.getY() || pos.getZ() >= size.getZ()) {
                diagnostics.add("blocks[" + i + "] is outside declared size: " + pos);
                continue;
            }
            int stateIndex = entry.getInteger("state");
            if (stateIndex < 0 || stateIndex >= states.size()) {
                diagnostics.add("blocks[" + i + "] references invalid palette index " + stateIndex + "; barrier used");
                stateIndex = -1;
            }
            NBTTagCompound tile = entry.hasKey("nbt", 10) ? entry.getCompoundTag("nbt") : null;
            PonderStructure.BlockInfo previous = blocks.put(pos, new PonderStructure.BlockInfo(pos,
                stateIndex < 0 ? net.minecraft.init.Blocks.BARRIER.getDefaultState() : states.get(stateIndex).getState(), tile));
            if (previous != null) diagnostics.add("duplicate block position " + pos + "; last entry retained");
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
            entities.add(new PonderStructure.EntityInfo(new Vec3d(pos[0], pos[1], pos[2]), entry.getCompoundTag("nbt")));
        }
        return new PonderStructure(size, new ArrayList<PonderStructure.BlockInfo>(blocks.values()), entities, diagnostics);
    }

    private static int[] readIntVector(NBTTagCompound parent, String key) throws IOException {
        if (!parent.hasKey(key, 9)) throw new IOException("Missing list '" + key + "'");
        NBTTagList list = parent.getTagList(key, 3);
        if (list.tagCount() != 3) throw new IOException("List '" + key + "' must have exactly 3 integers");
        return new int[] { list.getIntAt(0), list.getIntAt(1), list.getIntAt(2) };
    }

    private static double[] readDoubleVector(NBTTagCompound parent, String key) throws IOException {
        NBTTagList list = parent.getTagList(key, 6);
        if (list.tagCount() != 3) throw new IOException("List '" + key + "' must have exactly 3 doubles");
        return new double[] { list.getDoubleAt(0), list.getDoubleAt(1), list.getDoubleAt(2) };
    }

    private static IOException malformed(ResourceLocation source, String message) {
        return new IOException("Malformed Ponder structure " + source + ": " + message);
    }
}
