package net.createmod.ponder.foundation.element;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.createmod.ponder.api.element.TrackedElement;

public abstract class TrackedElementBase<T> extends PonderElementBase implements TrackedElement<T> {
    @Nullable protected T tracked;
    protected TrackedElementBase(@Nullable T tracked){this.tracked=tracked;}
    @Override public void ifPresent(Consumer<T> consumer){if(tracked!=null&&isStillValid(tracked))consumer.accept(tracked);}
}
