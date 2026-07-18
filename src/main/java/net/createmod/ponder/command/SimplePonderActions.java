package net.createmod.ponder.command;

import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.ponder.Ponder;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.ui.PonderIndexScreen;
import net.createmod.ponder.foundation.ui.PonderTagIndexScreen;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.util.ResourceLocation;
import net.createmod.ponder.foundation.structure.PonderStructureLoader;
import net.createmod.ponder.foundation.diagnostic.PonderDiagnosticService;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class SimplePonderActions {
    private SimplePonderActions() {
    }

    public static void openPonder(String value) {
        if ("index".equals(value) || "ponder:index".equals(value)) {
            ScreenOpener.open(new PonderIndexScreen());
            return;
        }
        if ("tags".equals(value) || "ponder:tags".equals(value)) {
            ScreenOpener.open(new PonderTagIndexScreen());
            return;
        }
        ResourceLocation id;
        try { id = new ResourceLocation(value); }
        catch (RuntimeException malformed) {
            Ponder.LOGGER.error("Invalid Ponder scene id from server: {}", value);
            return;
        }
        if (!PonderIndex.getSceneAccess().doScenesExistForId(id)) {
            Ponder.LOGGER.error("Could not find Ponder scenes for {}", id);
            return;
        }
        ScreenOpener.open(PonderUI.of(id));
    }

    public static void reloadPonder(String ignored) {
        PonderStructureLoader.invalidateCaches();
        PonderIndex.reload();
    }

    public static void diagnostic(String request) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null)
            return;
        try {
            PonderDiagnosticService.execute("client", request,
                message -> minecraft.player.sendMessage(new TextComponentString(message)));
        } catch (RuntimeException failure) {
            minecraft.player.sendMessage(
                new TextComponentTranslation("ponder.diagnostic.command_failed", failure.getMessage()));
        }
    }
}
