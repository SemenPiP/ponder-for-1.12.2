package net.createmod.ponder.verification;

import net.createmod.ponder.Ponder;
import net.createmod.ponder.PonderBootstrap;
import net.createmod.ponder.client.ClientProxy;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Development-only FML entry point. It supplies the lifecycle that a coremod-only
 * runClient classpath does not deliver to Ponder's regular @Mod container.
 */
@SideOnly(Side.CLIENT)
@Mod(
    modid = PonderClientHarnessMod.MOD_ID,
    name = "Ponder Client Acceptance Harness",
    version = "1.2.0",
    acceptedMinecraftVersions = "[1.12.2]",
    dependencies = "required-after:forge@[14.23.5.2847,);required-after:mixinbooter@[9.1,);"
        + "required-after:crafttweaker@[4.1.20,);after:ponder_legacy",
    acceptableRemoteVersions = "*",
    clientSideOnly = true
)
public final class PonderClientHarnessMod {
    public static final String MOD_ID = "ponder_client_harness";

    private ClientSmokeController controller;
    private ClientProxy manualProxy;
    private boolean manualLifecycle;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        controller = new ClientSmokeController();
        MinecraftForge.EVENT_BUS.register(controller);
        manualLifecycle = !Loader.isModLoaded(Ponder.MOD_ID);
        if (!manualLifecycle) return;
        try {
            manualProxy = new ClientProxy();
            manualProxy.preInit(event);
        } catch (Throwable throwable) {
            controller.failDuringLifecycle("pre_init", throwable);
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!manualLifecycle || controller == null || controller.hasFailed()) return;
        try {
            manualProxy.init(event);
        } catch (Throwable throwable) {
            controller.failDuringLifecycle("init", throwable);
        }
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        if (!manualLifecycle || controller == null || controller.hasFailed()) return;
        try {
            manualProxy.postInit(event);
        } catch (Throwable throwable) {
            controller.failDuringLifecycle("post_init", throwable);
        }
    }

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        if (controller == null || controller.hasFailed()) return;
        try {
            // Idempotent when the normal Ponder mod container was also discovered.
            PonderBootstrap.finishRegistration();
            if (manualLifecycle) manualProxy.loadComplete(event);
            controller.arm(manualLifecycle);
        } catch (Throwable throwable) {
            controller.failDuringLifecycle("load_complete", throwable);
        }
    }
}
