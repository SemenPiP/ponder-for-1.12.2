package net.createmod.ponder.foundation.element;

import java.util.UUID;

import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.PonderElement;

public final class ElementLinkImpl<T extends PonderElement> implements ElementLink<T> {
    private final Class<T> type;
    private final UUID id;

    public ElementLinkImpl(Class<T> type) {
        this(type, UUID.randomUUID());
    }

    public ElementLinkImpl(Class<T> type, UUID id) {
        if (type == null || id == null)
            throw new IllegalArgumentException("Element link type and id are required");
        this.type = type;
        this.id = id;
    }

    @Override public UUID getId() { return id; }
    @Override public T cast(PonderElement element) { return type.cast(element); }

    @Override public boolean equals(Object obj) {
        return obj == this || obj instanceof ElementLinkImpl && id.equals(((ElementLinkImpl<?>) obj).id);
    }

    @Override public int hashCode() { return id.hashCode(); }
    @Override public String toString() { return "ElementLink{" + type.getSimpleName() + ":" + id + "}"; }
}
