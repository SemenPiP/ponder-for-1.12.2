package net.createmod.ponder.foundation.content;

import net.createmod.ponder.Ponder;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.util.ResourceLocation;

/** Development-only plugin installed by PonderClient in deobfuscated environments. */
public final class DebugPonderPlugin implements PonderPlugin {
    @Override public String getModId() { return Ponder.CONTENT_NAMESPACE; }
    @Override public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) { DebugScenes.registerAll(helper); }
}
