package net.createmod.ponder.mmce;

import net.createmod.ponder.api.structure.PonderStructureProviders;
import net.createmod.ponder.api.subject.PonderSubjectResolvers;
import net.createmod.ponder.mmce.structure.MMCEStructureProvider;
import net.createmod.ponder.mmce.subject.MMCEBlueprintResolver;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(
    modid = PonderMMCE.MOD_ID,
    name = PonderMMCE.MOD_NAME,
    version = PonderMMCE.VERSION,
    acceptedMinecraftVersions = "[1.12.2]",
    dependencies = "required-after:forge@[14.23.5.2847,);required-after:ponder_legacy@[1.1.2,);"
        + "required-after:modularmachinery@[2.3.2,);required-after:crafttweaker@[4.1.20,)"
)
public final class PonderMMCEMod {
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        if (!MMCECompatibility.isSupported()) return;
        PonderStructureProviders.register(MMCEStructureProvider.INSTANCE);
        PonderSubjectResolvers.register(PonderMMCE.BLUEPRINT_RESOLVER_ID,
            MMCEBlueprintResolver.PRIORITY, MMCEBlueprintResolver.INSTANCE);
    }
}
