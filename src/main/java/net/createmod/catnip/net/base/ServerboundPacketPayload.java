package net.createmod.catnip.net.base;

import net.minecraft.entity.player.EntityPlayerMP;

public interface ServerboundPacketPayload extends BasePacketPayload { void handle(EntityPlayerMP player); }
