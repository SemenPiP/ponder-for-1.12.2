package net.createmod.ponder.client;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.createmod.ponder.PonderClient;
import net.createmod.ponder.foundation.PonderTooltipHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.createmod.ponder.script.ScriptSceneSync;
import net.createmod.ponder.script.ScriptMissingStructures;
import net.createmod.ponder.script.net.ScriptSnapshotReceiver;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class PonderClientEvents {
    private static final List<ResizeListener> RESIZE_LISTENERS = new CopyOnWriteArrayList<ResizeListener>();

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        PonderClient.onTick();
        PonderTooltipHandler.tick();
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player != null) {
            for (String notice : ScriptMissingStructures.drain())
                minecraft.player.sendMessage(new TextComponentString(notice));
        }
    }

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        PonderTooltipHandler.onTooltip(event);
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        Entity cameraEntity = minecraft.getRenderViewEntity();
        if (cameraEntity == null) {
            return;
        }
        float partial = event.getPartialTicks();
        Vec3d camera = new Vec3d(
            cameraEntity.lastTickPosX + (cameraEntity.posX - cameraEntity.lastTickPosX) * partial,
            cameraEntity.lastTickPosY + (cameraEntity.posY - cameraEntity.lastTickPosY) * partial,
            cameraEntity.lastTickPosZ + (cameraEntity.posZ - cameraEntity.lastTickPosZ) * partial);
        PonderClient.GHOST_BLOCKS.renderAll(camera);
        net.createmod.catnip.outliner.Outliner.getInstance().renderOutlines(camera, partial);
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        ScriptSnapshotReceiver.reset();
        ScriptSceneSync.clearServerScenes();
    }

    public static void onWindowResize(int width, int height) {
        for (ResizeListener listener : RESIZE_LISTENERS) {
            listener.onResize(width, height);
        }
    }

    public static void addResizeListener(ResizeListener listener) {
        if (listener != null) {
            RESIZE_LISTENERS.add(listener);
        }
    }

    public static void removeResizeListener(ResizeListener listener) {
        RESIZE_LISTENERS.remove(listener);
    }

    public interface ResizeListener {
        void onResize(int width, int height);
    }
}
