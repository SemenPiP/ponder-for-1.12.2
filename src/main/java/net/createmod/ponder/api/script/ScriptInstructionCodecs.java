package net.createmod.ponder.api.script;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

public final class ScriptInstructionCodecs {
    private static final Map<ResourceLocation, ScriptInstructionCodec> CODECS =
        new LinkedHashMap<ResourceLocation, ScriptInstructionCodec>();
    private static final Map<ResourceLocation, ScriptInstructionCodecDescriptor> DESCRIPTORS =
        new LinkedHashMap<ResourceLocation, ScriptInstructionCodecDescriptor>();

    private ScriptInstructionCodecs() {
    }

    public static synchronized void register(ScriptInstructionCodec codec) {
        if (codec == null || codec.getId() == null) throw new IllegalArgumentException("Script codec and id are required");
        if (CODECS.containsKey(codec.getId()))
            throw new IllegalArgumentException("Duplicate Ponder script instruction codec " + codec.getId());
        ScriptInstructionCodecDescriptor descriptor = descriptor(codec);
        CODECS.put(codec.getId(), codec);
        DESCRIPTORS.put(codec.getId(), descriptor);
    }

    public static synchronized ScriptInstructionCodec get(ResourceLocation id) {
        return CODECS.get(id);
    }

    public static synchronized Map<ResourceLocation, ScriptInstructionCodec> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<ResourceLocation, ScriptInstructionCodec>(CODECS));
    }

    public static synchronized ScriptInstructionCodecDescriptor getDescriptor(ResourceLocation id) {
        return DESCRIPTORS.get(id);
    }

    public static synchronized Map<ResourceLocation, ScriptInstructionCodecDescriptor> descriptorSnapshot() {
        return Collections.unmodifiableMap(
            new LinkedHashMap<ResourceLocation, ScriptInstructionCodecDescriptor>(DESCRIPTORS));
    }

    private static ScriptInstructionCodecDescriptor descriptor(ScriptInstructionCodec codec) {
        return new ScriptInstructionCodecDescriptor(codec.getId(), codec.getProtocolVersion(),
            codec.getCapabilities());
    }
}
