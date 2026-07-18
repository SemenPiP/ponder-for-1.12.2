package net.createmod.ponder.script;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.createmod.ponder.api.script.ScriptInstructionCodecDescriptor;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public final class ScriptSceneSnapshot {
    public static final int PROTOCOL = 3;
    public static final int MAX_UNCOMPRESSED_BYTES = 16 * 1024 * 1024;
    public static final int MAX_COMPRESSED_BYTES = 16 * 1024 * 1024;
    public static final int MAX_SCENE_BYTES = 1024 * 1024;
    public static final int MAX_REQUIRED_CODECS = 256;
    public static final int MAX_TAGS = 1024;
    public static final int MAX_TAG_COMPONENTS = 8192;
    public static final int MAX_SHARED_TEXT = 4096;
    public static final int MAX_TEXT_LENGTH = 8192;

    private ScriptSceneSnapshot() {
    }

    public static Encoded encode(List<ScriptSceneDefinition> scenes) throws IOException {
        return encode(scenes, Collections.<ScriptTagDefinition>emptyList(),
            Collections.<String, String>emptyMap());
    }

    public static Encoded encodeLocal(List<ScriptSceneDefinition> scenes) throws IOException {
        return encode(scenes, ScriptTagRegistry.localSnapshot(),
            ScriptSharedText.localSnapshot());
    }

    static Encoded encode(List<ScriptSceneDefinition> scenes,
                          Collection<ScriptTagDefinition> tags,
                          Map<String, String> sharedText) throws IOException {
        if (scenes.size() > ScriptSceneRegistry.MAX_SCENES)
            throw new IOException("Scene snapshot exceeds " + ScriptSceneRegistry.MAX_SCENES + " scenes");
        List<ScriptTagDefinition> validatedTags = validateTags(tags);
        Map<String, String> validatedSharedText = validateSharedText(sharedText);
        List<ScriptInstructionCodecDescriptor> requirements =
            ScriptCodecDescriptors.requirements(scenes);
        NBTTagCompound root = new NBTTagCompound();
        root.setInteger("protocol", PROTOCOL);
        root.setTag("codecRequirements", ScriptCodecDescriptors.toNbt(requirements));
        root.setTag("tags", serializeTags(validatedTags));
        root.setTag("sharedText", serializeSharedText(validatedSharedText));
        NBTTagList list = new NBTTagList();
        for (ScriptSceneDefinition scene : scenes) {
            NBTTagCompound serialized = scene.serialize();
            int sceneBytes = uncompressedSize(serialized);
            if (sceneBytes > MAX_SCENE_BYTES)
                throw new IOException("Scene " + scene.getSceneId() + " exceeds " + MAX_SCENE_BYTES + " bytes");
            list.appendTag(serialized);
        }
        root.setTag("scenes", list);
        int uncompressed = uncompressedSize(root);
        if (uncompressed > MAX_UNCOMPRESSED_BYTES)
            throw new IOException("Scene snapshot exceeds " + MAX_UNCOMPRESSED_BYTES + " uncompressed bytes");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CompressedStreamTools.writeCompressed(root, output);
        byte[] bytes = output.toByteArray();
        if (bytes.length > MAX_COMPRESSED_BYTES)
            throw new IOException("Scene snapshot exceeds " + MAX_COMPRESSED_BYTES + " compressed bytes");
        return new Encoded(bytes, uncompressed, sha256(bytes), requirements);
    }

    public static List<ScriptSceneDefinition> decode(byte[] compressed, int expectedUncompressed) throws IOException {
        return decodeContent(compressed, expectedUncompressed).scenes;
    }

    public static Decoded decodeContent(byte[] compressed, int expectedUncompressed) throws IOException {
        if (compressed.length > MAX_COMPRESSED_BYTES)
            throw new IOException("Compressed scene snapshot is too large");
        NBTTagCompound root = CompressedStreamTools.readCompressed(new ByteArrayInputStream(compressed));
        if (root.getInteger("protocol") != PROTOCOL)
            throw new IOException("Unsupported scene protocol " + root.getInteger("protocol"));
        int actual = uncompressedSize(root);
        if (actual != expectedUncompressed || actual > MAX_UNCOMPRESSED_BYTES)
            throw new IOException("Scene snapshot uncompressed size mismatch");
        NBTTagList list = root.getTagList("scenes", 10);
        if (list.tagCount() > ScriptSceneRegistry.MAX_SCENES)
            throw new IOException("Scene snapshot contains too many scenes");
        List<ScriptInstructionCodecDescriptor> declaredRequirements;
        List<ScriptTagDefinition> tags;
        Map<String, String> sharedText;
        try {
            declaredRequirements = ScriptCodecDescriptors.fromNbt(
                root.getTagList("codecRequirements", 10));
            tags = deserializeTags(root.getTagList("tags", 10));
            sharedText = deserializeSharedText(root.getTagList("sharedText", 10));
        } catch (RuntimeException malformed) {
            throw new IOException("Invalid scene snapshot metadata: "
                + malformed.getMessage(), malformed);
        }
        List<ScriptSceneDefinition> result = new ArrayList<ScriptSceneDefinition>();
        for (int i = 0; i < list.tagCount(); i++) {
            try {
                NBTTagCompound serialized = list.getCompoundTagAt(i);
                int sceneBytes = uncompressedSize(serialized);
                if (sceneBytes > MAX_SCENE_BYTES)
                    throw new IOException("Scene #" + i + " exceeds " + MAX_SCENE_BYTES + " bytes");
                result.add(ScriptSceneDefinition.deserialize(serialized));
            } catch (RuntimeException malformed) {
                throw new IOException("Invalid scene #" + i + ": " + malformed.getMessage(), malformed);
            }
        }
        List<ScriptInstructionCodecDescriptor> derivedRequirements =
            ScriptCodecDescriptors.requirements(result);
        if (!declaredRequirements.equals(derivedRequirements))
            throw new IOException("Scene snapshot codec requirements do not match scene instructions");
        return new Decoded(result, declaredRequirements, tags, sharedText);
    }

    public static List<ScriptInstructionCodecDescriptor> requiredCodecs(
            List<ScriptSceneDefinition> scenes) throws IOException {
        return ScriptCodecDescriptors.requirements(scenes);
    }

    public static byte[] sha256(byte[] bytes) throws IOException {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IOException("SHA-256 unavailable", impossible);
        }
    }

    static int uncompressedSize(NBTTagCompound root) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(output);
        CompressedStreamTools.write(root, data);
        data.flush();
        return output.size();
    }

    private static NBTTagList serializeTags(Collection<ScriptTagDefinition> tags) {
        NBTTagList list = new NBTTagList();
        for (ScriptTagDefinition tag : tags) {
            NBTTagCompound value = new NBTTagCompound();
            value.setString("id", tag.id.toString());
            value.setString("icon", tag.icon.toString());
            value.setString("title", tag.title);
            value.setString("description", tag.description);
            value.setBoolean("indexed", tag.indexed);
            NBTTagList components = new NBTTagList();
            for (net.minecraft.util.ResourceLocation component : tag.components) {
                NBTTagCompound encoded = new NBTTagCompound();
                encoded.setString("id", component.toString());
                components.appendTag(encoded);
            }
            value.setTag("components", components);
            list.appendTag(value);
        }
        return list;
    }

    private static List<ScriptTagDefinition> deserializeTags(NBTTagList list) {
        if (list == null || list.tagCount() > MAX_TAGS)
            throw new IllegalArgumentException("Scene snapshot contains too many tags");
        List<ScriptTagDefinition> result = new ArrayList<ScriptTagDefinition>(list.tagCount());
        int componentCount = 0;
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound value = list.getCompoundTagAt(i);
            NBTTagList components = value.getTagList("components", 10);
            componentCount += components.tagCount();
            if (componentCount > MAX_TAG_COMPONENTS)
                throw new IllegalArgumentException("Scene snapshot contains too many tag component associations");
            List<net.minecraft.util.ResourceLocation> componentIds =
                new ArrayList<net.minecraft.util.ResourceLocation>(components.tagCount());
            for (int j = 0; j < components.tagCount(); j++)
                componentIds.add(new net.minecraft.util.ResourceLocation(
                    components.getCompoundTagAt(j).getString("id")));
            result.add(new ScriptTagDefinition(
                new net.minecraft.util.ResourceLocation(value.getString("id")),
                new net.minecraft.util.ResourceLocation(value.getString("icon")),
                value.getString("title"), value.getString("description"),
                value.getBoolean("indexed"), componentIds));
        }
        return validateTags(result);
    }

    private static NBTTagList serializeSharedText(Map<String, String> sharedText) {
        NBTTagList list = new NBTTagList();
        for (Map.Entry<String, String> entry : sharedText.entrySet()) {
            NBTTagCompound value = new NBTTagCompound();
            value.setString("key", entry.getKey());
            value.setString("value", entry.getValue());
            list.appendTag(value);
        }
        return list;
    }

    private static Map<String, String> deserializeSharedText(NBTTagList list) {
        if (list == null || list.tagCount() > MAX_SHARED_TEXT)
            throw new IllegalArgumentException("Scene snapshot contains too many shared text entries");
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound value = list.getCompoundTagAt(i);
            String key = value.getString("key");
            if (result.put(key, value.getString("value")) != null)
                throw new IllegalArgumentException("Duplicate shared text key in scene snapshot: " + key);
        }
        return validateSharedText(result);
    }

    private static List<ScriptTagDefinition> validateTags(Collection<ScriptTagDefinition> tags) {
        if (tags == null || tags.size() > MAX_TAGS)
            throw new IllegalArgumentException("Invalid Ponder script tag collection");
        Map<net.minecraft.util.ResourceLocation, ScriptTagDefinition> unique =
            new LinkedHashMap<net.minecraft.util.ResourceLocation, ScriptTagDefinition>();
        int componentCount = 0;
        for (ScriptTagDefinition tag : tags) {
            if (tag == null || unique.put(tag.id, tag) != null)
                throw new IllegalArgumentException("Duplicate or null Ponder script tag");
            componentCount += tag.components.size();
            if (componentCount > MAX_TAG_COMPONENTS)
                throw new IllegalArgumentException("Too many Ponder script tag component associations");
        }
        return Collections.unmodifiableList(new ArrayList<ScriptTagDefinition>(unique.values()));
    }

    private static Map<String, String> validateSharedText(Map<String, String> sharedText) {
        if (sharedText == null || sharedText.size() > MAX_SHARED_TEXT)
            throw new IllegalArgumentException("Invalid Ponder shared text collection");
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String> entry : sharedText.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || key.trim().isEmpty() || key.length() > 256)
                throw new IllegalArgumentException("Shared text key is missing or too long");
            if (value == null || value.length() > MAX_TEXT_LENGTH)
                throw new IllegalArgumentException("Shared text value is missing or too long: " + key);
            result.put(key, value);
        }
        return Collections.unmodifiableMap(result);
    }

    public static final class Encoded {
        public final byte[] bytes;
        public final int uncompressedBytes;
        public final byte[] hash;
        public final List<ScriptInstructionCodecDescriptor> requirements;

        Encoded(byte[] bytes, int uncompressedBytes, byte[] hash,
                List<ScriptInstructionCodecDescriptor> requirements) {
            this.bytes = bytes;
            this.uncompressedBytes = uncompressedBytes;
            this.hash = hash;
            this.requirements = requirements;
        }
    }

    public static final class Decoded {
        public final List<ScriptSceneDefinition> scenes;
        public final List<ScriptInstructionCodecDescriptor> requirements;
        final List<ScriptTagDefinition> tags;
        final Map<String, String> sharedText;

        Decoded(List<ScriptSceneDefinition> scenes,
                List<ScriptInstructionCodecDescriptor> requirements,
                List<ScriptTagDefinition> tags, Map<String, String> sharedText) {
            this.scenes = Collections.unmodifiableList(
                new ArrayList<ScriptSceneDefinition>(scenes));
            this.requirements = requirements;
            this.tags = tags;
            this.sharedText = sharedText;
        }

        public int getSceneCount() {
            return scenes.size();
        }

        public int getTagCount() {
            return tags.size();
        }

        public int getSharedTextCount() {
            return sharedText.size();
        }
    }
}
