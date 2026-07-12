package net.createmod.catnip.net;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.createmod.catnip.net.packets.ClientboundSimpleActionPacket;

public final class SimpleCatnipActions {
    private SimpleCatnipActions(){}
    public static void register(String name,Supplier<Consumer<String>> action){ClientboundSimpleActionPacket.addAction(name,action);}
}
