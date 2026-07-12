package net.createmod.catnip.event;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public abstract class ClientResourceReloadListener implements IResourceManagerReloadListener {
    @Override
    public final void onResourceManagerReload(IResourceManager resourceManager) {
        onReload(resourceManager);
    }

    protected abstract void onReload(IResourceManager resourceManager);
}
