package net.createmod.ponder.mmce;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(
    modid = PonderMMCE.MOD_ID,
    name = PonderMMCE.MOD_NAME,
    version = PonderMMCE.VERSION,
    acceptedMinecraftVersions = "[1.12.2]",
    dependencies = "required-after:forge@[14.23.5.2847,);required-after:ponder_legacy@[1.1.2,);"
        + "after:modularmachinery;required-after:crafttweaker@[4.1.20,)"
)
public final class PonderMMCEMod {
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        PonderMMCEEntrypoint.preInit();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        PonderMMCEEntrypoint.postInit();
    }
}
