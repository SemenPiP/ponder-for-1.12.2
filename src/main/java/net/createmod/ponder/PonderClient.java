package net.createmod.ponder;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.ghostblock.GhostBlocks;
import net.createmod.catnip.config.ui.ConfigScreenOpener;
import net.createmod.catnip.net.CatnipPackets;
import net.createmod.catnip.net.packets.ClientboundSimpleActionPacket;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.placement.PlacementClient;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.client.ClientPonderElementFactory;
import net.createmod.ponder.command.SimplePonderActions;
import net.createmod.ponder.foundation.PonderElementFactories;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.content.DebugPonderPlugin;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class PonderClient {
    public static final GhostBlocks GHOST_BLOCKS = GhostBlocks.getInstance();
    private static boolean initialized;
    private PonderClient() {}
    public static synchronized void init() {
        if (initialized) return; initialized = true; CatnipPackets.register();
        PonderElementFactories.set(new ClientPonderElementFactory());
        ClientboundSimpleActionPacket.addAction("openPonder", () -> SimplePonderActions::openPonder);
        ClientboundSimpleActionPacket.addAction("reloadPonder", () -> SimplePonderActions::reloadPonder);
        ClientboundSimpleActionPacket.addAction("ponderDiagnostic", () -> SimplePonderActions::diagnostic);
        ClientboundSimpleActionPacket.addAction("configScreen", () -> ConfigScreenOpener::open);
        if (CatnipServices.PLATFORM.isDevelopmentEnvironment()) PonderIndex.addPlugin(new DebugPonderPlugin());
    }
    public static void modLoadCompleted() { PonderIndex.registerAll(); }
    public static void onTick() {
        AnimationTickHolder.tick();
        GHOST_BLOCKS.tickGhosts();
        if (!isGameActive()) return;
        if (Minecraft.getMinecraft().currentScreen == null) PlacementClient.tick();
        Outliner.getInstance().tickOutlines();
    }
    public static void invalidateRenderers() {
        if (Minecraft.getMinecraft().currentScreen instanceof PonderUI)
            ((PonderUI) Minecraft.getMinecraft().currentScreen).getActiveScene()
                .forEach(WorldSectionElement.class, WorldSectionElement::queueRedraw);
    }
    public static boolean isGameActive() { return Minecraft.getMinecraft().world != null && Minecraft.getMinecraft().player != null; }
}
