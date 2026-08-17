package net.createmod.ponder.mmce.verification;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
@Mod(
    modid = PonderMMCEClientHarness.MOD_ID,
    name = "Ponder-MMCE Client Harness",
    version = "0.1.0-alpha",
    acceptedMinecraftVersions = "[1.12.2]",
    dependencies = "required-after:ponder_legacy@[1.3.0-alpha.1-mc1.12.2];"
        + "required-after:modularmachinery@[2.3.2,)",
    acceptableRemoteVersions = "*",
    clientSideOnly = true
)
public final class PonderMMCEClientHarness {
    public static final String MOD_ID = "ponder_mmce_client_harness";

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        PonderMMCEClientSmokeController controller = new PonderMMCEClientSmokeController();
        MinecraftForge.EVENT_BUS.register(controller);
        controller.arm();
    }
}
