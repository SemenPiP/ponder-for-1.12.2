package net.createmod.ponder.foundation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import net.createmod.ponder.Ponder;
import net.createmod.ponder.api.registration.LangRegistryAccess;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.SceneRegistryAccess;
import net.createmod.ponder.api.registration.TagRegistryAccess;
import net.createmod.ponder.enums.PonderConfig;
import net.createmod.ponder.foundation.content.BasePonderPlugin;
import net.createmod.ponder.foundation.registration.DefaultPonderSceneRegistrationHelper;
import net.createmod.ponder.foundation.registration.DefaultPonderTagRegistrationHelper;
import net.createmod.ponder.foundation.registration.DefaultSharedTextRegistrationHelper;
import net.createmod.ponder.foundation.registration.PonderIndexExclusionHelper;
import net.createmod.ponder.foundation.registration.PonderLocalization;
import net.createmod.ponder.foundation.registration.PonderSceneRegistry;
import net.createmod.ponder.foundation.registration.PonderTagRegistry;
import net.createmod.ponder.command.PonderCommands;
import net.createmod.ponder.script.ScriptPonderPlugin;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

public final class PonderIndex {
    private static final List<PonderPlugin> PLUGINS = new ArrayList<PonderPlugin>();
    private static final Set<String> PLUGIN_CLASSES = new HashSet<String>();
    private static volatile RegistrationState state = RegistrationState.empty();
    private static volatile PonderLocalization.TranslationProvider translationProvider;
    private static boolean discovered;
    private static boolean frozen;

    private static final Comparator<PonderPlugin> PLUGIN_ORDER = new Comparator<PonderPlugin>() {
        @Override public int compare(PonderPlugin left, PonderPlugin right) {
            int base = Boolean.compare(!Ponder.CONTENT_NAMESPACE.equals(left.getModId()),
                !Ponder.CONTENT_NAMESPACE.equals(right.getModId()));
            if (base != 0) return base;
            int mod = left.getModId().compareTo(right.getModId());
            return mod != 0 ? mod : left.getClass().getName().compareTo(right.getClass().getName());
        }
    };

    static {
        addPlugin(new BasePonderPlugin());
        addPlugin(new ScriptPonderPlugin());
    }

    private PonderIndex() {
    }

    public static synchronized void addPlugin(PonderPlugin plugin) {
        if (plugin == null) throw new IllegalArgumentException("Ponder plugin may not be null");
        if (frozen) throw new IllegalStateException("Ponder plugin discovery is complete");
        if (plugin.getModId() == null || plugin.getModId().trim().isEmpty())
            throw new IllegalArgumentException("Ponder plugin returned a blank mod id: " + plugin.getClass().getName());
        if (!PLUGIN_CLASSES.add(plugin.getClass().getName())) return;
        PLUGINS.add(plugin);
        Collections.sort(PLUGINS, PLUGIN_ORDER);
    }

    public static synchronized void discoverPlugins() {
        if (discovered) return;
        discovered = true;
        try {
            for (PonderPlugin plugin : ServiceLoader.load(PonderPlugin.class, PonderIndex.class.getClassLoader()))
                addPlugin(plugin);
        } catch (ServiceConfigurationError error) {
            Ponder.LOGGER.error("A Ponder ServiceLoader plugin could not be created", error);
        }
    }

    public static synchronized void registerAll() {
        RegistrationState replacement = buildState();
        state = replacement;
        frozen = true;
        Ponder.LOGGER.info("Registered {} Ponder plugin(s) and {} storyboard(s)", PLUGINS.size(),
            replacement.scenes.getRegisteredEntries().size());
    }

    public static synchronized void gatherSharedText() {
        registerAll();
    }

    public static synchronized void reload() {
        registerAll();
    }

    private static RegistrationState buildState() {
        PonderLocalization localization = new PonderLocalization();
        localization.setTranslationProvider(translationProvider);
        PonderSceneRegistry scenes = new PonderSceneRegistry(localization);
        PonderTagRegistry tags = new PonderTagRegistry();
        PonderIndexExclusionHelper exclusions = new PonderIndexExclusionHelper();

        for (PonderPlugin plugin : PLUGINS)
            plugin.registerScenes(new DefaultPonderSceneRegistrationHelper(plugin.getModId(), scenes));
        for (PonderPlugin plugin : PLUGINS)
            plugin.registerTags(new DefaultPonderTagRegistrationHelper(plugin.getModId(), tags, localization));
        for (PonderPlugin plugin : PLUGINS)
            plugin.registerSharedText(new DefaultSharedTextRegistrationHelper(plugin.getModId(), localization));
        for (PonderPlugin plugin : PLUGINS)
            plugin.indexExclusions(exclusions);

        scenes.freeze();
        tags.freeze();
        return new RegistrationState(localization, scenes, tags, exclusions);
    }

    public static void forEachPlugin(Consumer<PonderPlugin> consumer) {
        for (PonderPlugin plugin : pluginSnapshot()) consumer.accept(plugin);
    }

    public static Stream<PonderPlugin> streamPlugins() {
        return pluginSnapshot().stream();
    }

    private static synchronized List<PonderPlugin> pluginSnapshot() {
        return new ArrayList<PonderPlugin>(PLUGINS);
    }

    public static SceneRegistryAccess getSceneAccess() { return state.scenes; }
    public static TagRegistryAccess getTagAccess() { return state.tags; }
    public static LangRegistryAccess getLangAccess() { return state.localization; }
    public static void setTranslationProvider(PonderLocalization.TranslationProvider provider) {
        translationProvider = provider;
        state.localization.setTranslationProvider(provider);
    }
    public static PonderIndexExclusionHelper getIndexExclusions() { return state.exclusions; }
    public static boolean editingModeActive() { return PonderConfig.client().isEditingMode(); }

    public static void registerCommands(FMLServerStartingEvent event) {
        PonderCommands.register(event);
    }

    private static final class RegistrationState {
        final PonderLocalization localization;
        final PonderSceneRegistry scenes;
        final PonderTagRegistry tags;
        final PonderIndexExclusionHelper exclusions;

        RegistrationState(PonderLocalization localization, PonderSceneRegistry scenes,
                          PonderTagRegistry tags, PonderIndexExclusionHelper exclusions) {
            this.localization = localization;
            this.scenes = scenes;
            this.tags = tags;
            this.exclusions = exclusions;
        }

        static RegistrationState empty() {
            PonderLocalization localization = new PonderLocalization();
            PonderSceneRegistry scenes = new PonderSceneRegistry(localization);
            PonderTagRegistry tags = new PonderTagRegistry();
            scenes.freeze();
            tags.freeze();
            return new RegistrationState(localization, scenes, tags, new PonderIndexExclusionHelper());
        }
    }
}
