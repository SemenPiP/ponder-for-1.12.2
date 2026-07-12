package net.createmod.catnip.levelWrappers;

import java.util.Locale;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public final class WorldHelper {
    private WorldHelper() {
    }

    public static ResourceLocation getDimensionID(IBlockAccess access) {
        if (access instanceof WrappedLevel) access = ((WrappedLevel) access).getLevel();
        if (!(access instanceof World)) return new ResourceLocation("minecraft", "unknown");
        World world = (World) access;
        String name = world.provider.getDimensionType().getName().toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9_./-]", "_");
        if (name.isEmpty()) name = "dimension_" + world.provider.getDimension();
        return new ResourceLocation("minecraft", name);
    }
}
