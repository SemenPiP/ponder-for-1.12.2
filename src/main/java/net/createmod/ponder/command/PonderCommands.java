package net.createmod.ponder.command;

import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

public final class PonderCommands {
    private PonderCommands() {
    }

    public static void register(FMLServerStartingEvent event) {
        if (event == null) throw new IllegalArgumentException("Server starting event is required");
        event.registerServerCommand(new PonderCommand());
    }
}
