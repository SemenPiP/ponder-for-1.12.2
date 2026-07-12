package net.createmod.ponder.api;

/** Implemented by tile entities which need to know that they run in a virtual scene world. */
public interface VirtualBlockEntity {
    void markVirtual();

    boolean isVirtual();
}
