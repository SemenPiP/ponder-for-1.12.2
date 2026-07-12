package net.createmod.ponder.foundation.registration;

import net.createmod.ponder.api.registration.SharedTextRegistrationHelper;
import net.minecraft.util.ResourceLocation;

public final class DefaultSharedTextRegistrationHelper implements SharedTextRegistrationHelper {
    private final String namespace;
    private final PonderLocalization localization;

    public DefaultSharedTextRegistrationHelper(String namespace, PonderLocalization localization) {
        this.namespace = namespace;
        this.localization = localization;
    }

    @Override public void registerSharedText(String key, String enUs) {
        localization.registerShared(new ResourceLocation(namespace, key), enUs);
    }
}
