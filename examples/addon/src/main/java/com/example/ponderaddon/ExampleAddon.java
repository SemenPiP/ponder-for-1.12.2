package com.example.ponderaddon;

import net.minecraftforge.fml.common.Mod;

/** Minimal host mod; the Ponder plugin itself is discovered through ServiceLoader. */
@Mod(modid = ExampleAddon.MOD_ID, name = "Ponder Example Addon", version = "1.1.0",
    dependencies = "required-after:forge@[14.23.5.2847,);required-after:ponder_legacy@[1.1.0-mc1.12.2]")
public final class ExampleAddon {
    public static final String MOD_ID = "ponder_example";
}
