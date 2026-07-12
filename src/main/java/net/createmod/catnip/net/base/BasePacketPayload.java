package net.createmod.catnip.net.base;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public interface BasePacketPayload extends IMessage {
    PacketTypeProvider getTypeProvider();
    interface PacketTypeProvider { CatnipPacketRegistry.PacketType<?> getPacketType(); }
}
