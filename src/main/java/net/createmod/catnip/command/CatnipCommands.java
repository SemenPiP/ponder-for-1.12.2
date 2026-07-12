package net.createmod.catnip.command;

import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

public final class CatnipCommands {
    private CatnipCommands(){}
    public static void register(FMLServerStartingEvent event){event.registerServerCommand(new CatnipCommand());}
}
