package net.createmod.ponder;

import java.util.Map;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkCheckHandler;
import net.minecraftforge.fml.relauncher.Side;

@Mod(
    modid = Ponder.MOD_ID,
    name = Ponder.MOD_NAME,
    version = Ponder.VERSION,
    acceptedMinecraftVersions = "[1.12.2]",
    dependencies = "required-after:forge@[14.23.5.2847,);required-after:mixinbooter@[9.1,);"
        + "required-after:crafttweaker@[4.1.20,)",
    guiFactory = "net.createmod.ponder.client.PonderGuiFactory",
    acceptableRemoteVersions = "*"
)
public final class PonderMod {

    @Mod.Instance(Ponder.MOD_ID)
    public static PonderMod instance;

    @SidedProxy(
        clientSide = "net.createmod.ponder.client.ClientProxy",
        serverSide = "net.createmod.ponder.CommonProxy"
    )
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Ponder.LOGGER.info("Starting {} {} for Minecraft 1.12.2", Ponder.MOD_NAME, Ponder.VERSION);
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void processImc(FMLInterModComms.IMCEvent event) {
        PonderBootstrap.processImc(event);
    }

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        PonderBootstrap.finishRegistration();
        proxy.loadComplete(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        PonderBootstrap.registerCommands(event);
    }

    @NetworkCheckHandler
    public boolean checkRemoteVersions(Map<String, String> remoteVersions, Side remoteSide) {
        return true;
    }
}
