package net.createmod.ponder.client;

import java.io.File;

import net.createmod.catnip.config.ConfigRegistry;
import net.createmod.catnip.config.ConfigType;
import net.createmod.catnip.event.ClientResourceReloadListener;
import net.createmod.catnip.ghostblock.GhostBlocks;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.render.SuperByteBufferCache;
import net.createmod.ponder.CommonProxy;
import net.createmod.ponder.PonderClient;
import net.createmod.ponder.Ponder;
import net.createmod.ponder.config.CClient;
import net.createmod.ponder.enums.PonderKeybinds;
import net.createmod.ponder.foundation.PonderElementFactories;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.render.SectionRenderCache;
import net.createmod.ponder.foundation.structure.PonderStructureLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {
    private final PonderClientEvents events = new PonderClientEvents();

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        File configFile = new File(event.getModConfigurationDirectory(), "ponder-client.cfg");
        if (ConfigRegistry.get(Ponder.MOD_ID, ConfigType.CLIENT) == null) {
            ConfigRegistry.register(Ponder.MOD_ID, ConfigType.CLIENT, new CClient(), configFile);
        }
        PonderKeybinds.register(ClientRegistry::registerKeyBinding);
        MinecraftForge.EVENT_BUS.register(events);
        FMLCommonHandler.instance().bus().register(events);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        PonderClient.init();
        PonderElementFactories.set(new ClientPonderElementFactory());
        PonderIndex.setTranslationProvider(new net.createmod.ponder.foundation.registration.PonderLocalization.TranslationProvider() {
            @Override
            public String translate(String key, Object... parameters) {
                return I18n.format(key, parameters);
            }
        });
        IResourceManager manager = Minecraft.getMinecraft().getResourceManager();
        PonderStructureLoader.setResourceProvider(location -> {
            try {
                return Minecraft.getMinecraft().getResourceManager().getResource(location).getInputStream();
            } catch (java.io.FileNotFoundException missing) {
                return null;
            }
        });
        if (manager instanceof IReloadableResourceManager) {
            ((IReloadableResourceManager) manager).registerReloadListener(new ClientResourceReloadListener() {
                @Override
                protected void onReload(IResourceManager resourceManager) {
                    GhostBlocks.getInstance().clear();
                    Outliner.getInstance().clear();
                    SuperByteBufferCache.getInstance().invalidate();
                    SectionRenderCache.invalidateAll();
                    PonderClient.invalidateRenderers();
                    PonderStructureLoader.invalidateCaches();
                }
            });
        }
    }
}
