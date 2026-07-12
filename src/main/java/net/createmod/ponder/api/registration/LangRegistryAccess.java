package net.createmod.ponder.api.registration;

import java.util.function.BiConsumer;

import net.minecraft.util.ResourceLocation;

public interface LangRegistryAccess {
    void provideLang(String modId, BiConsumer<String, String> consumer);

    String getShared(ResourceLocation key);

    String getShared(ResourceLocation key, Object... params);

    String getTagName(ResourceLocation key);

    String getTagDescription(ResourceLocation key);

    String getSpecific(ResourceLocation sceneId, String key);

    String getSpecific(ResourceLocation sceneId, String key, Object... params);
}
