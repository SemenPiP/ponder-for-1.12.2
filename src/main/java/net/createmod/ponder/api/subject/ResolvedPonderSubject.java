package net.createmod.ponder.api.subject;

import net.minecraft.util.ResourceLocation;

public final class ResolvedPonderSubject {
    private static final ResolvedPonderSubject PASS =
        new ResolvedPonderSubject(false, null, null, false);

    private final boolean handled;
    private final ResourceLocation component;
    private final ResourceLocation resolverId;
    private final boolean defaultResolver;

    private ResolvedPonderSubject(boolean handled, ResourceLocation component,
                                 ResourceLocation resolverId, boolean defaultResolver) {
        this.handled = handled;
        this.component = component;
        this.resolverId = resolverId;
        this.defaultResolver = defaultResolver;
    }

    static ResolvedPonderSubject pass() {
        return PASS;
    }

    static ResolvedPonderSubject handled(ResourceLocation component, ResourceLocation resolverId,
                                         boolean defaultResolver) {
        return new ResolvedPonderSubject(true, component, resolverId, defaultResolver);
    }

    public boolean isHandled() {
        return handled;
    }

    public ResourceLocation getComponent() {
        if (!handled) throw new IllegalStateException("Unresolved subject has no component id");
        return component;
    }

    public ResourceLocation getResolverId() {
        if (!handled) throw new IllegalStateException("Unresolved subject has no resolver id");
        return resolverId;
    }

    public boolean isDefaultResolver() {
        return handled && defaultResolver;
    }
}
