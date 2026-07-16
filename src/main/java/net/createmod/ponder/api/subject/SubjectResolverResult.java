package net.createmod.ponder.api.subject;

import net.minecraft.util.ResourceLocation;

public final class SubjectResolverResult {
    public enum Status {
        PASS,
        HANDLED
    }

    public static final SubjectResolverResult PASS =
        new SubjectResolverResult(Status.PASS, null);

    private final Status status;
    private final ResourceLocation component;

    private SubjectResolverResult(Status status, ResourceLocation component) {
        this.status = status;
        this.component = component;
    }

    public static SubjectResolverResult pass() {
        return PASS;
    }

    public static SubjectResolverResult handled(ResourceLocation component) {
        if (component == null)
            throw new IllegalArgumentException("Handled subject resolution requires a component id");
        return new SubjectResolverResult(Status.HANDLED, component);
    }

    public Status getStatus() {
        return status;
    }

    public boolean isHandled() {
        return status == Status.HANDLED;
    }

    public ResourceLocation getComponent() {
        if (!isHandled())
            throw new IllegalStateException("PASS subject resolution has no component id");
        return component;
    }
}
