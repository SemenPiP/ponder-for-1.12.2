package net.createmod.ponder;

import java.lang.reflect.Constructor;

import net.createmod.catnip.command.CatnipCommands;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

/** Coordinates plugin discovery without forcing client classes onto a dedicated server. */
public final class PonderBootstrap {

    public static final String REGISTER_PLUGIN_MESSAGE = "register_ponder_plugin";
    private static boolean finished;

    private PonderBootstrap() {
    }

    public static void processImc(FMLInterModComms.IMCEvent event) {
        for (FMLInterModComms.IMCMessage message : event.getMessages()) {
            if (!REGISTER_PLUGIN_MESSAGE.equals(message.key) || !message.isStringMessage()) {
                continue;
            }
            registerPluginClass(message.getStringValue(), message.getSender());
        }
    }

    private static void registerPluginClass(String className, String sender) {
        try {
            Class<?> pluginClass = Class.forName(className, true, PonderBootstrap.class.getClassLoader());
            if (!PonderPlugin.class.isAssignableFrom(pluginClass)) {
                throw new IllegalArgumentException(className + " does not implement PonderPlugin");
            }
            Constructor<?> constructor = pluginClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            PonderIndex.addPlugin((PonderPlugin) constructor.newInstance());
        } catch (ReflectiveOperationException | RuntimeException exception) {
            Ponder.LOGGER.error("Could not register Ponder plugin {} sent by {}", className, sender, exception);
        }
    }

    public static synchronized void finishRegistration() {
        if (finished) {
            return;
        }
        PonderIndex.discoverPlugins();
        PonderIndex.registerAll();
        finished = true;
    }

    public static void registerCommands(FMLServerStartingEvent event) {
        CatnipCommands.register(event);
        PonderIndex.registerCommands(event);
    }
}
