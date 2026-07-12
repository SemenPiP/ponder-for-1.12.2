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
import net.createmod.ponder.foundation.content.BasePonderPlugin;

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

    private static final class ZetaModPlugin implements PonderPlugin {
        @Override public String getModId() { return "zeta"; }
    }

    private static final class SameModAlphaPlugin implements PonderPlugin {
        @Override public String getModId() { return "same"; }
    }

    private static final class SameModZuluPlugin implements PonderPlugin {
        @Override public String getModId() { return "same"; }
    }
}
