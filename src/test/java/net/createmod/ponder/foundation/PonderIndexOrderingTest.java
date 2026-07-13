package net.createmod.ponder.foundation;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.SceneRegistryAccess;
import net.createmod.ponder.foundation.content.BasePonderPlugin;
import net.minecraft.util.ResourceLocation;

public class PonderIndexOrderingTest {
    @Test
    @SuppressWarnings("unchecked")
    public void pluginsSortBaseFirstThenByModIdAndClassName() throws Exception {
        Field field = PonderIndex.class.getDeclaredField("PLUGIN_ORDER");
        field.setAccessible(true);
        Comparator<PonderPlugin> order = (Comparator<PonderPlugin>) field.get(null);

        PonderPlugin base = new BasePonderPlugin();
        PonderPlugin zeta = new ZetaModPlugin();
        PonderPlugin sameZulu = new SameModZuluPlugin();
        PonderPlugin sameAlpha = new SameModAlphaPlugin();
        List<PonderPlugin> plugins = new ArrayList<PonderPlugin>();
        Collections.addAll(plugins, zeta, sameZulu, base, sameAlpha);
        Collections.sort(plugins, order);

        assertSame(base, plugins.get(0));
        assertSame(sameAlpha, plugins.get(1));
        assertSame(sameZulu, plugins.get(2));
        assertSame(zeta, plugins.get(3));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void addPluginDeduplicatesAndFreezeRejectsLateRegistration() throws Exception {
        Field pluginsField = PonderIndex.class.getDeclaredField("PLUGINS");
        Field classesField = PonderIndex.class.getDeclaredField("PLUGIN_CLASSES");
        Field frozenField = PonderIndex.class.getDeclaredField("frozen");
        pluginsField.setAccessible(true);
        classesField.setAccessible(true);
        frozenField.setAccessible(true);
        List<PonderPlugin> plugins = (List<PonderPlugin>) pluginsField.get(null);
        Set<String> classes = (Set<String>) classesField.get(null);
        List<PonderPlugin> pluginSnapshot = new ArrayList<PonderPlugin>(plugins);
        Set<String> classSnapshot = new java.util.HashSet<String>(classes);
        boolean frozenSnapshot = frozenField.getBoolean(null);
        try {
            plugins.clear();
            classes.clear();
            frozenField.setBoolean(null, false);
            PonderPlugin first = new SameModAlphaPlugin();
            PonderIndex.addPlugin(first);
            PonderIndex.addPlugin(new SameModAlphaPlugin());
            PonderIndex.addPlugin(new ZetaModPlugin());
            assertEquals(2, plugins.size());
            assertSame(first, plugins.get(0));

            frozenField.setBoolean(null, true);
            try {
                PonderIndex.addPlugin(new SameModZuluPlugin());
                fail("Frozen plugin discovery accepted a late registration");
            } catch (IllegalStateException expected) {
                // Required load-complete freeze behavior.
            }
        } finally {
            plugins.clear();
            plugins.addAll(pluginSnapshot);
            classes.clear();
            classes.addAll(classSnapshot);
            frozenField.setBoolean(null, frozenSnapshot);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void failedReloadKeepsPreviousRegistrationState() throws Exception {
        Field pluginsField = PonderIndex.class.getDeclaredField("PLUGINS");
        Field classesField = PonderIndex.class.getDeclaredField("PLUGIN_CLASSES");
        Field frozenField = PonderIndex.class.getDeclaredField("frozen");
        Field stateField = PonderIndex.class.getDeclaredField("state");
        pluginsField.setAccessible(true);
        classesField.setAccessible(true);
        frozenField.setAccessible(true);
        stateField.setAccessible(true);
        List<PonderPlugin> plugins = (List<PonderPlugin>) pluginsField.get(null);
        Set<String> classes = (Set<String>) classesField.get(null);
        List<PonderPlugin> pluginSnapshot = new ArrayList<PonderPlugin>(plugins);
        Set<String> classSnapshot = new java.util.HashSet<String>(classes);
        boolean frozenSnapshot = frozenField.getBoolean(null);
        Object stateSnapshot = stateField.get(null);
        try {
            plugins.clear();
            classes.clear();
            frozenField.setBoolean(null, false);
            PonderIndex.addPlugin(new BasePonderPlugin());
            PonderIndex.registerAll();
            SceneRegistryAccess previousScenes = PonderIndex.getSceneAccess();

            plugins.add(new ThrowingPlugin());
            try {
                PonderIndex.reload();
                fail("Failed registration unexpectedly replaced the live Ponder state");
            } catch (IllegalStateException expected) {
                assertEquals("fixture failure", expected.getMessage());
            }
            assertSame(previousScenes, PonderIndex.getSceneAccess());
        } finally {
            plugins.clear();
            plugins.addAll(pluginSnapshot);
            classes.clear();
            classes.addAll(classSnapshot);
            frozenField.setBoolean(null, frozenSnapshot);
            stateField.set(null, stateSnapshot);
        }
    }

    private static final class ZetaModPlugin implements PonderPlugin {
        @Override public String getModId() { return "zeta"; }
    }

    private static final class SameModAlphaPlugin implements PonderPlugin {
        @Override public String getModId() { return "same"; }
    }

    private static final class ThrowingPlugin implements PonderPlugin {
        @Override public String getModId() { return "throwing"; }
        @Override
        public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
            throw new IllegalStateException("fixture failure");
        }
    }

    private static final class SameModZuluPlugin implements PonderPlugin {
        @Override public String getModId() { return "same"; }
    }
}
