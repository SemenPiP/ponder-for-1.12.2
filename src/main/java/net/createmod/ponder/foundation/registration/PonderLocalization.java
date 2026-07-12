package net.createmod.ponder.foundation.registration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

import net.createmod.ponder.api.registration.LangRegistryAccess;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.util.ResourceLocation;

/** Stores source-language text and delegates runtime translation without linking client-only I18n classes. */
public final class PonderLocalization implements LangRegistryAccess {
    public interface TranslationProvider {
        String translate(String key, Object... parameters);
    }

    private static final TranslationProvider FALLBACK_PROVIDER = new TranslationProvider() {
        @Override
        public String translate(String key, Object... parameters) {
            return key;
        }
    };

    private final Map<ResourceLocation, String> shared = new LinkedHashMap<ResourceLocation, String>();
    private final Map<ResourceLocation, TagText> tags = new LinkedHashMap<ResourceLocation, TagText>();
    private final Map<ResourceLocation, Map<String, String>> specific =
        new LinkedHashMap<ResourceLocation, Map<String, String>>();
    private volatile TranslationProvider translationProvider = FALLBACK_PROVIDER;

    public void setTranslationProvider(TranslationProvider provider) {
        translationProvider = provider == null ? FALLBACK_PROVIDER : provider;
    }

    public synchronized void clearAll() {
        shared.clear();
        tags.clear();
        specific.clear();
    }

    public synchronized void clearShared() {
        shared.clear();
    }

    public synchronized void registerShared(ResourceLocation key, String english) {
        requireText(key, english);
        shared.put(key, english);
    }

    public synchronized void registerTag(ResourceLocation key, String title, String description) {
        requireText(key, title);
        if (description == null)
            throw new IllegalArgumentException("Tag description may not be null");
        tags.put(key, new TagText(title, description));
    }

    public synchronized void registerSpecific(ResourceLocation sceneId, String key, String english) {
        requireText(sceneId, english);
        if (key == null || key.trim().isEmpty())
            throw new IllegalArgumentException("Scene text key may not be blank");
        Map<String, String> scene = specific.get(sceneId);
        if (scene == null) {
            scene = new LinkedHashMap<String, String>();
            specific.put(sceneId, scene);
        }
        scene.put(key, english);
    }

    private static void requireText(ResourceLocation key, String text) {
        if (key == null || text == null)
            throw new IllegalArgumentException("Localization key and text are required");
    }

    public static String langKeyForShared(ResourceLocation key) {
        return key.getNamespace() + ".ponder.shared." + key.getPath();
    }

    public static String langKeyForTag(ResourceLocation key) {
        return key.getNamespace() + ".ponder.tag." + key.getPath();
    }

    public static String langKeyForTagDescription(ResourceLocation key) {
        return langKeyForTag(key) + ".description";
    }

    public static String langKeyForSpecific(ResourceLocation sceneId, String key) {
        return sceneId.getNamespace() + ".ponder." + sceneId.getPath() + "." + key;
    }

    @Override
    public String getShared(ResourceLocation key) {
        return resolve(langKeyForShared(key), value(shared, key, "unregistered shared entry: " + key));
    }

    @Override
    public String getShared(ResourceLocation key, Object... params) {
        return resolve(langKeyForShared(key), value(shared, key, "unregistered shared entry: " + key), params);
    }

    @Override
    public String getTagName(ResourceLocation key) {
        TagText value = tags.get(key);
        return resolve(langKeyForTag(key), value == null ? "unregistered tag: " + key : value.title);
    }

    @Override
    public String getTagDescription(ResourceLocation key) {
        TagText value = tags.get(key);
        return resolve(langKeyForTagDescription(key), value == null ? "unregistered tag: " + key : value.description);
    }

    @Override
    public String getSpecific(ResourceLocation sceneId, String key) {
        return resolve(langKeyForSpecific(sceneId, key), specificValue(sceneId, key));
    }

    @Override
    public String getSpecific(ResourceLocation sceneId, String key, Object... params) {
        return resolve(langKeyForSpecific(sceneId, key), specificValue(sceneId, key), params);
    }

    private String specificValue(ResourceLocation sceneId, String key) {
        Map<String, String> values = specific.get(sceneId);
        return values == null || !values.containsKey(key) ? "missing scene text: " + sceneId + "/" + key : values.get(key);
    }

    private static String value(Map<ResourceLocation, String> map, ResourceLocation key, String missing) {
        String value = map.get(key);
        return value == null ? missing : value;
    }

    private String resolve(String languageKey, String fallback, Object... parameters) {
        String translated = translationProvider.translate(languageKey, parameters);
        String selected = translated == null || translated.equals(languageKey) ? fallback : translated;
        if (parameters == null || parameters.length == 0)
            return selected;
        try {
            return String.format(Locale.ROOT, selected, parameters);
        } catch (RuntimeException ignored) {
            return selected;
        }
    }

    @Override
    public synchronized void provideLang(String modId, BiConsumer<String, String> consumer) {
        if (modId == null || consumer == null)
            throw new IllegalArgumentException("Mod id and consumer are required");
        PonderIndex.getSceneAccess().getRegisteredEntries().forEach(entry ->
            PonderSceneRegistry.compileScene(this, entry.getValue(), null));
        for (Map.Entry<ResourceLocation, String> entry : shared.entrySet())
            if (modId.equals(entry.getKey().getNamespace()))
                consumer.accept(langKeyForShared(entry.getKey()), entry.getValue());
        for (Map.Entry<ResourceLocation, TagText> entry : tags.entrySet()) {
            if (!modId.equals(entry.getKey().getNamespace()))
                continue;
            consumer.accept(langKeyForTag(entry.getKey()), entry.getValue().title);
            consumer.accept(langKeyForTagDescription(entry.getKey()), entry.getValue().description);
        }
        for (Map.Entry<ResourceLocation, Map<String, String>> scene : specific.entrySet())
            if (modId.equals(scene.getKey().getNamespace()))
                for (Map.Entry<String, String> text : scene.getValue().entrySet())
                    consumer.accept(langKeyForSpecific(scene.getKey(), text.getKey()), text.getValue());
    }

    public Map<ResourceLocation, String> getSharedDefaults() {
        return Collections.unmodifiableMap(shared);
    }

    private static final class TagText {
        final String title;
        final String description;

        TagText(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }
}
