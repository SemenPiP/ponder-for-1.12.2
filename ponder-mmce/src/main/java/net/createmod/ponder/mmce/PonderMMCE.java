package net.createmod.ponder.mmce;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.util.ResourceLocation;

public final class PonderMMCE {
    public static final String MOD_ID = "ponder_mmce";
    public static final String MOD_NAME = "Ponder-MMCE";
    public static final String VERSION = "0.1.0-alpha";
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);
    public static final ResourceLocation PROVIDER_ID = new ResourceLocation(MOD_ID, "mmce");
    public static final ResourceLocation BLUEPRINT_RESOLVER_ID = new ResourceLocation(MOD_ID, "blueprint");

    private PonderMMCE() {
    }
}
