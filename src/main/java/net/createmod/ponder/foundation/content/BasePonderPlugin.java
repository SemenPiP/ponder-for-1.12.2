package net.createmod.ponder.foundation.content;

import net.createmod.ponder.Ponder;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.SharedTextRegistrationHelper;
import net.minecraft.util.ResourceLocation;

/** Vanilla-only reference plugin proving the public registration and scene DSL end to end. */
public final class BasePonderPlugin implements PonderPlugin {
    public static final ResourceLocation BASICS = id("basics");
    static final ResourceLocation STORAGE = id("storage");
    static final ResourceLocation MECHANICS = id("mechanics");
    static final ResourceLocation REDSTONE = id("redstone");
    static final ResourceLocation RENDERING = id("rendering");

    @Override public String getModId() { return Ponder.CONTENT_NAMESPACE; }

    @Override
    public void registerSharedText(SharedTextRegistrationHelper helper) {
        helper.registerSharedText("demo.controls", "Drag to rotate and scroll to zoom");
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(Ponder.CONTENT_NAMESPACE, path);
    }
}
