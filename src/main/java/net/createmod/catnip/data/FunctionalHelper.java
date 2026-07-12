package net.createmod.catnip.data;

import java.util.function.Function;

public final class FunctionalHelper {
    private FunctionalHelper() {}
    public static <U> Function<Object, U> filterAndCast(final Class<? extends U> type) {
        return new Function<Object, U>() {
            public U apply(Object value) { return type.isInstance(value) ? type.cast(value) : null; }
        };
    }
}
