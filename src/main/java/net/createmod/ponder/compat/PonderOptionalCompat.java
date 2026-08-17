package net.createmod.ponder.compat;

import java.lang.reflect.Method;

import net.createmod.ponder.Ponder;
import net.minecraftforge.fml.common.Loader;

/**
 * Loads optional integrations without putting dependency types on the core
 * lifecycle or dedicated-server class path.
 */
public final class PonderOptionalCompat {
    private static final String MMCE_MOD_ID = "modularmachinery";
    private static final String EXTERNAL_MMCE_MOD_ID = "ponder_mmce";
    private static final String MMCE_ENTRYPOINT =
        "net.createmod.ponder.mmce.PonderMMCEEntrypoint";

    private static boolean attempted;

    private PonderOptionalCompat() {
    }

    public static synchronized void preInit() {
        if (attempted || !Loader.isModLoaded(MMCE_MOD_ID)) return;
        attempted = true;
        if (Loader.isModLoaded(EXTERNAL_MMCE_MOD_ID)) {
            Ponder.LOGGER.info(
                "MMCE detected with the legacy ponder_mmce addon; leaving ownership to that addon");
            return;
        }
        invoke("preInit");
    }

    public static synchronized void postInit() {
        if (!attempted || Loader.isModLoaded(EXTERNAL_MMCE_MOD_ID)) return;
        invoke("postInit");
    }

    private static void invoke(String methodName) {
        try {
            Class<?> entrypoint = Class.forName(MMCE_ENTRYPOINT, true,
                PonderOptionalCompat.class.getClassLoader());
            Method method = entrypoint.getMethod(methodName);
            method.invoke(null);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
            Ponder.LOGGER.error(
                "Optional MMCE integration could not be enabled; Ponder will continue without it",
                failure);
        }
    }
}
