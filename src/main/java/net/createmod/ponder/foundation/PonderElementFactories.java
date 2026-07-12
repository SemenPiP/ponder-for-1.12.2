package net.createmod.ponder.foundation;

public final class PonderElementFactories {
    private static volatile PonderElementFactory factory = new HeadlessPonderElementFactory();

    private PonderElementFactories() {
    }

    public static PonderElementFactory get() {
        return factory;
    }

    public static void set(PonderElementFactory replacement) {
        if (replacement == null)
            throw new IllegalArgumentException("Ponder element factory may not be null");
        factory = replacement;
    }
}
