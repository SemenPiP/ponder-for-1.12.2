package net.createmod.catnip.platform.services;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

public interface ModFluidHelper<R> {
    int getColor(R fluid); int getLuminosity(R fluid); boolean isLighterThanAir(R fluid); R toStack(Fluid fluid,int amount);
}
