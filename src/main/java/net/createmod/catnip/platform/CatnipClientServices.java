package net.createmod.catnip.platform;

import net.createmod.catnip.platform.services.ModClientHooksHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Client-only service holder for the single Forge 1.12.2 platform implementation. */
@SideOnly(Side.CLIENT)
public final class CatnipClientServices {
    public static final ModClientHooksHelper CLIENT_HOOKS = new ForgeClientHooksHelper();

    private CatnipClientServices() {
    }
}
