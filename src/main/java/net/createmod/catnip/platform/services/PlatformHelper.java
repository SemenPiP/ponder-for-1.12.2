package net.createmod.catnip.platform.services;

import java.util.List;
import java.util.function.Supplier;

import net.createmod.catnip.platform.Env;
import net.createmod.catnip.platform.Loader;

public interface PlatformHelper {
    Loader getLoader(); Env getEnv(); boolean isModLoaded(String modId); boolean isDevelopmentEnvironment();
    List<String> getLoadedMods(); String getModDisplayName(String modId);
    void executeOnClientOnly(Supplier<Runnable> action); void executeOnServerOnly(Supplier<Runnable> action);
}
