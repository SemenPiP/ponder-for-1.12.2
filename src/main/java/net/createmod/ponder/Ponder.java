package net.createmod.ponder;

import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.util.ResourceLocation;

public final class Ponder {

    public static final String MOD_ID = "ponder_legacy";
    public static final String CONTENT_NAMESPACE = "ponder";
    public static final String MOD_NAME = "Ponder";
    public static final String VERSION = "1.1.0-mc1.12.2";
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);
    public static final Random RANDOM = new Random();

    private Ponder() {
    }

    public static ResourceLocation asResource(String path) {
        return new ResourceLocation(CONTENT_NAMESPACE, path);
    }
}
