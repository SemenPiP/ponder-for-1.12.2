package net.createmod.catnip.levelWrappers;

import javax.annotation.Nullable;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Client-safe IBlockAccess delegate; it does not manufacture a second client world. */
@SideOnly(Side.CLIENT)
public class WrappedClientLevel extends WrappedLevel {
    public WrappedClientLevel(IBlockAccess level) {
        super(level);
    }

    public static WrappedClientLevel of(IBlockAccess level) {
        return new WrappedClientLevel(level);
    }

    @Nullable
    public WorldClient getClientWorld() {
        return level instanceof WorldClient ? (WorldClient) level : null;
    }
}
