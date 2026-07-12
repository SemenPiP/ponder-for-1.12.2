package net.createmod.catnip.platform;

import net.createmod.catnip.platform.services.ModFluidHelper;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

public class ForgeFluidHelper implements ModFluidHelper<FluidStack> {
    public int getColor(FluidStack stack){return stack.getFluid().getColor(stack);}
    public int getLuminosity(FluidStack stack){return stack.getFluid().getLuminosity(stack);}
    public boolean isLighterThanAir(FluidStack stack){return stack.getFluid().isGaseous(stack);}
    public FluidStack toStack(Fluid fluid,int amount){return new FluidStack(fluid,Math.max(0,amount));}
}
