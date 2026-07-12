package net.createmod.ponder.enums;

import java.util.function.Consumer;

import org.lwjgl.input.Keyboard;

import net.createmod.catnip.client.ConflictSafeKeyMapping;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public enum PonderKeybinds {
    PONDER("ponder", Keyboard.KEY_W);

    public static final String CATEGORY = "key.categories.ponder";
    private final KeyBinding mapping;

    PonderKeybinds(String description, int defaultKey) {
        mapping = new ConflictSafeKeyMapping("key.ponder." + description, defaultKey, CATEGORY);
    }

    public static void register(Consumer<KeyBinding> callback) {
        for (PonderKeybinds keybind : values()) callback.accept(keybind.mapping);
    }

    public KeyBinding getMapping() { return mapping; }
    public boolean isDown() { return mapping.getKeyCode() != Keyboard.KEY_NONE && mapping.isKeyDown(); }
    public ITextComponent message() { return new TextComponentString(GameSettings.getKeyDisplayString(mapping.getKeyCode())); }
}
