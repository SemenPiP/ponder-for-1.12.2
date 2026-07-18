package net.createmod.ponder.script;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.netty.buffer.ByteBuf;
import net.createmod.ponder.api.script.ScriptInstructionCodec;
import net.createmod.ponder.api.script.ScriptInstructionCodecDescriptor;
import net.createmod.ponder.api.script.ScriptInstructionCodecs;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.ByteBufUtils;

/** Internal deterministic serialization and validation for codec capability descriptors. */
public final class ScriptCodecDescriptors {
    private static final Comparator<ScriptInstructionCodecDescriptor> ORDER =
        Comparator.comparing(descriptor -> descriptor.getId().toString());

    private ScriptCodecDescriptors() {
    }

    public static List<ScriptInstructionCodecDescriptor> localCapabilities() {
        return sorted(ScriptInstructionCodecs.descriptorSnapshot().values());
    }

    public static ScriptInstructionCodecDescriptor requirement(ScriptInstructionCodec codec,
                                                               NBTTagCompound payload) {
        if (codec == null)
            throw new IllegalArgumentException("Script instruction codec is required");
        ScriptInstructionCodecDescriptor supported =
            ScriptInstructionCodecs.getDescriptor(codec.getId());
        if (supported == null)
            supported = new ScriptInstructionCodecDescriptor(codec.getId(), codec.getProtocolVersion(),
                codec.getCapabilities());
        Set<ResourceLocation> required = codec.getRequiredCapabilities(
            payload == null ? new NBTTagCompound() : payload.copy());
        ScriptInstructionCodecDescriptor requirement =
            new ScriptInstructionCodecDescriptor(codec.getId(), supported.getProtocolVersion(), required);
        if (!supported.satisfies(requirement))
            throw new IllegalArgumentException("Script instruction codec " + codec.getId()
                + " requires capabilities it does not advertise: " + requirement.getCapabilities());
        return requirement;
    }

    public static List<ScriptInstructionCodecDescriptor> requirements(
            Collection<ScriptSceneDefinition> scenes) throws java.io.IOException {
        Map<ResourceLocation, MutableRequirement> merged =
            new LinkedHashMap<ResourceLocation, MutableRequirement>();
        for (ScriptSceneDefinition scene : scenes) {
            for (ScriptInstruction instruction : scene.getInstructions()) {
                if (!"custom".equals(instruction.getOperation()))
                    continue;
                ResourceLocation id = new ResourceLocation(instruction.getData().getString("codec"));
                ScriptInstructionCodec codec = ScriptInstructionCodecs.get(id);
                if (codec == null)
                    throw new java.io.IOException("Scene " + scene.getSceneId()
                        + " requires unavailable codec " + id);
                ScriptInstructionCodecDescriptor requirement;
                try {
                    requirement = requirement(codec,
                        instruction.getData().getCompoundTag("payload"));
                } catch (RuntimeException invalid) {
                    throw new java.io.IOException("Scene " + scene.getSceneId()
                        + " has invalid codec requirement " + id + ": " + invalid.getMessage(), invalid);
                }
                MutableRequirement current = merged.get(id);
                if (current == null) {
                    current = new MutableRequirement(requirement.getProtocolVersion());
                    merged.put(id, current);
                } else if (current.protocolVersion != requirement.getProtocolVersion()) {
                    throw new java.io.IOException("Codec " + id
                        + " changed protocol version while encoding a scene snapshot");
                }
                current.capabilities.addAll(requirement.getCapabilities());
                if (merged.size() > ScriptSceneSnapshot.MAX_REQUIRED_CODECS)
                    throw new java.io.IOException("Scene snapshot requires more than "
                        + ScriptSceneSnapshot.MAX_REQUIRED_CODECS + " codecs");
            }
        }
        List<ScriptInstructionCodecDescriptor> result =
            new ArrayList<ScriptInstructionCodecDescriptor>(merged.size());
        for (Map.Entry<ResourceLocation, MutableRequirement> entry : merged.entrySet())
            result.add(new ScriptInstructionCodecDescriptor(entry.getKey(),
                entry.getValue().protocolVersion, entry.getValue().capabilities));
        return sorted(result);
    }

    public static List<ScriptInstructionCodecDescriptor> validate(
            Collection<ScriptInstructionCodecDescriptor> descriptors) {
        Collection<ScriptInstructionCodecDescriptor> supplied = descriptors == null
            ? Collections.<ScriptInstructionCodecDescriptor>emptyList() : descriptors;
        if (supplied.size() > ScriptSceneSnapshot.MAX_REQUIRED_CODECS)
            throw new IllegalArgumentException("Too many Ponder script codec descriptors");
        LinkedHashSet<ResourceLocation> ids = new LinkedHashSet<ResourceLocation>();
        for (ScriptInstructionCodecDescriptor descriptor : supplied) {
            if (descriptor == null || !ids.add(descriptor.getId()))
                throw new IllegalArgumentException("Invalid or duplicate Ponder script codec descriptor");
        }
        return sorted(supplied);
    }

    public static Map<ResourceLocation, ScriptInstructionCodecDescriptor> byId(
            Collection<ScriptInstructionCodecDescriptor> descriptors) {
        Map<ResourceLocation, ScriptInstructionCodecDescriptor> result =
            new LinkedHashMap<ResourceLocation, ScriptInstructionCodecDescriptor>();
        for (ScriptInstructionCodecDescriptor descriptor : validate(descriptors))
            result.put(descriptor.getId(), descriptor);
        return Collections.unmodifiableMap(result);
    }

    public static NBTTagList toNbt(Collection<ScriptInstructionCodecDescriptor> descriptors) {
        NBTTagList list = new NBTTagList();
        for (ScriptInstructionCodecDescriptor descriptor : validate(descriptors)) {
            NBTTagCompound value = new NBTTagCompound();
            value.setString("id", descriptor.getId().toString());
            value.setInteger("version", descriptor.getProtocolVersion());
            NBTTagList capabilities = new NBTTagList();
            for (ResourceLocation capability : descriptor.getCapabilities()) {
                NBTTagCompound encoded = new NBTTagCompound();
                encoded.setString("id", capability.toString());
                capabilities.appendTag(encoded);
            }
            value.setTag("capabilities", capabilities);
            list.appendTag(value);
        }
        return list;
    }

    public static List<ScriptInstructionCodecDescriptor> fromNbt(NBTTagList list) {
        if (list == null || list.tagCount() > ScriptSceneSnapshot.MAX_REQUIRED_CODECS)
            throw new IllegalArgumentException("Invalid Ponder script codec descriptor list");
        List<ScriptInstructionCodecDescriptor> result =
            new ArrayList<ScriptInstructionCodecDescriptor>(list.tagCount());
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound value = list.getCompoundTagAt(i);
            NBTTagList capabilities = value.getTagList("capabilities", 10);
            List<ResourceLocation> capabilityIds = new ArrayList<ResourceLocation>(capabilities.tagCount());
            for (int j = 0; j < capabilities.tagCount(); j++)
                capabilityIds.add(new ResourceLocation(capabilities.getCompoundTagAt(j).getString("id")));
            result.add(new ScriptInstructionCodecDescriptor(
                new ResourceLocation(value.getString("id")), value.getInteger("version"), capabilityIds));
        }
        return validate(result);
    }

    public static void write(ByteBuf buffer, Collection<ScriptInstructionCodecDescriptor> descriptors) {
        List<ScriptInstructionCodecDescriptor> validated = validate(descriptors);
        buffer.writeShort(validated.size());
        for (ScriptInstructionCodecDescriptor descriptor : validated) {
            ByteBufUtils.writeUTF8String(buffer, descriptor.getId().toString());
            buffer.writeInt(descriptor.getProtocolVersion());
            buffer.writeByte(descriptor.getCapabilities().size());
            for (ResourceLocation capability : descriptor.getCapabilities())
                ByteBufUtils.writeUTF8String(buffer, capability.toString());
        }
    }

    public static List<ScriptInstructionCodecDescriptor> read(ByteBuf buffer) {
        int count = buffer.readUnsignedShort();
        if (count > ScriptSceneSnapshot.MAX_REQUIRED_CODECS)
            throw new IllegalArgumentException("Too many Ponder script codec descriptors");
        List<ScriptInstructionCodecDescriptor> result =
            new ArrayList<ScriptInstructionCodecDescriptor>(count);
        for (int i = 0; i < count; i++) {
            String id = bounded(ByteBufUtils.readUTF8String(buffer));
            int version = buffer.readInt();
            int capabilityCount = buffer.readUnsignedByte();
            if (capabilityCount > ScriptInstructionCodecDescriptor.MAX_CAPABILITIES)
                throw new IllegalArgumentException("Too many Ponder script codec capabilities");
            List<ResourceLocation> capabilities = new ArrayList<ResourceLocation>(capabilityCount);
            for (int j = 0; j < capabilityCount; j++)
                capabilities.add(new ResourceLocation(bounded(ByteBufUtils.readUTF8String(buffer))));
            result.add(new ScriptInstructionCodecDescriptor(new ResourceLocation(id), version, capabilities));
        }
        return validate(result);
    }

    private static String bounded(String value) {
        if (value == null || value.length() > ScriptInstructionCodecDescriptor.MAX_ID_LENGTH)
            throw new IllegalArgumentException("Ponder script codec resource id is too long");
        return value;
    }

    private static List<ScriptInstructionCodecDescriptor> sorted(
            Collection<ScriptInstructionCodecDescriptor> descriptors) {
        List<ScriptInstructionCodecDescriptor> result =
            new ArrayList<ScriptInstructionCodecDescriptor>(descriptors);
        Collections.sort(result, ORDER);
        return Collections.unmodifiableList(result);
    }

    private static final class MutableRequirement {
        final int protocolVersion;
        final Set<ResourceLocation> capabilities = new LinkedHashSet<ResourceLocation>();

        MutableRequirement(int protocolVersion) {
            this.protocolVersion = protocolVersion;
        }
    }
}
