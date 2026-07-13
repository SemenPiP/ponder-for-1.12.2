package net.createmod.ponder.script;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public final class ScriptSyncEvents {
    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP)
            ScriptSceneSync.requestCapabilities((EntityPlayerMP) event.player);
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player instanceof EntityPlayerMP)
            ScriptSceneSync.logout((EntityPlayerMP) event.player);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.side.isServer()
            && FMLCommonHandler.instance().getMinecraftServerInstance() != null)
            ScriptSceneSync.tick(FMLCommonHandler.instance().getMinecraftServerInstance());
    }
}
