package net.createmod.catnip.client;

import java.util.function.BooleanSupplier;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ConflictSafeKeyMapping extends KeyBinding {
    private final BooleanSupplier enabled;

    public ConflictSafeKeyMapping(String description, int keyCode, String category) {
        this(description, KeyConflictContext.IN_GAME, keyCode, category, new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return true;
            }
        });
    }

    public ConflictSafeKeyMapping(String description, IKeyConflictContext context, int keyCode,
                                  String category, BooleanSupplier enabled) {
        super(description, context, keyCode, category);
        this.enabled = enabled;
    }

    @Override
    public boolean isKeyDown() {
        return enabled.getAsBoolean() && super.isKeyDown();
    }

    @Override
    public boolean isPressed() {
        return enabled.getAsBoolean() && super.isPressed();
    }
}
