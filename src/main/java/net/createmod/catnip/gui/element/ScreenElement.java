package net.createmod.catnip.gui.element;

/**
 * Side-neutral description of something that can be drawn on a screen.
 *
 * <p>The interface deliberately contains no Minecraft client types. Public Ponder
 * scene builders use it as data, including while they are being discovered on a
 * dedicated server. Concrete renderers remain client-only.</p>
 */
public interface ScreenElement {
    void render(int x, int y);

    default void render(int x, int y, float alpha) {
        render(x, y);
    }
}
