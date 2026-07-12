package net.createmod.catnip.platform;

import net.createmod.catnip.platform.services.ModFluidHelper;
import net.createmod.catnip.platform.services.ModHooksHelper;
import net.createmod.catnip.platform.services.NetworkHelper;
import net.createmod.catnip.platform.services.PlatformHelper;

public final class CatnipServices {
    public static final PlatformHelper PLATFORM=new ForgePlatformHelper();
    public static final ModFluidHelper<?> FLUID_HELPER=new ForgeFluidHelper();
    public static final ModHooksHelper HOOKS=new ForgeHooksHelper();
    public static final NetworkHelper NETWORK=new ForgeNetworkHelper();
    private CatnipServices(){}
}
