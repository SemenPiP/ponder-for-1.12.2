package net.createmod.ponder.script;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    private ScriptSceneSnapshot() {
    }

    public static Encoded encode(List<ScriptSceneDefinition> scenes) throws IOException {
        if (scenes.size() > ScriptSceneRegistry.MAX_SCENES)
            throw new IOException("Scene snapshot exceeds " + ScriptSceneRegistry.MAX_SCENES + " scenes");
        List<ScriptInstructionCodecDescriptor> requirements =
            ScriptCodecDescriptors.requirements(scenes);
        NBTTagCompound root = new NBTTagCompound();
        root.setInteger("protocol", PROTOCOL);
        root.setTag("codecRequirements", ScriptCodecDescriptors.toNbt(requirements));
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
        try {
            declaredRequirements = ScriptCodecDescriptors.fromNbt(
                root.getTagList("codecRequirements", 10));
        } catch (RuntimeException malformed) {
            throw new IOException("Invalid scene snapshot codec requirements: "
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
        return new Decoded(result, declaredRequirements);
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

        Decoded(List<ScriptSceneDefinition> scenes,
                List<ScriptInstructionCodecDescriptor> requirements) {
            this.scenes = Collections.unmodifiableList(
                new ArrayList<ScriptSceneDefinition>(scenes));
            this.requirements = requirements;
        }
    }
}
