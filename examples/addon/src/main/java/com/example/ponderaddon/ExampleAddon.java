package com.example.ponderaddon;

import net.createmod.ponder.api.script.ScriptInstructionCodecs;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/** Host mod demonstrating codec, ServiceLoader and Forge IMC registration. */
@Mod(modid = ExampleAddon.MOD_ID, name = "Ponder Example Addon", version = "1.3.0-alpha.1",
    dependencies = "required-after:forge@[14.23.5.2847,);"
        + "required-after:ponder_legacy@[1.3.0-alpha.1-mc1.12.2]")
public final class ExampleAddon {
    public static final String MOD_ID = "ponder_example";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ScriptInstructionCodecs.register(new ExamplePulseCodec());
        FMLInterModComms.sendMessage("ponder_legacy", "register_ponder_plugin",
            ExampleImcPonderPlugin.class.getName());
    }
}
