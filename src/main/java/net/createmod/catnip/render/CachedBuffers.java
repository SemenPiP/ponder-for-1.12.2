package net.createmod.catnip.render;

import net.minecraft.block.state.IBlockState;
import net.createmod.catnip.render.SuperByteBufferCache.Compartment;

public final class CachedBuffers {
    public static final Compartment<IBlockState> GENERIC_BLOCK=new Compartment<IBlockState>();
    static{SuperByteBufferCache.getInstance().registerCompartment(GENERIC_BLOCK);}
    private CachedBuffers(){}
    public static SuperByteBuffer block(final IBlockState state){return SuperByteBufferCache.getInstance().get(GENERIC_BLOCK,state,new java.util.concurrent.Callable<SuperByteBuffer>(){@Override public SuperByteBuffer call(){return SuperBufferFactory.getInstance().createForBlock(state);}});}
}
