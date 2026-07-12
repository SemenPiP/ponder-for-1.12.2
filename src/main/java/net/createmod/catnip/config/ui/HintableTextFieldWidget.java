package net.createmod.catnip.config.ui;

import javax.annotation.Nullable;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** 1.12 text field with an unobtrusive hint when empty. */
@SideOnly(Side.CLIENT)
public class HintableTextFieldWidget extends GuiTextField {
    private final FontRenderer font;
    @Nullable private String hint;

    public HintableTextFieldWidget(int id, FontRenderer font, int x, int y, int width, int height) {
        super(id, font, x, y, width, height);
        this.font = font;
        setMaxStringLength(128);
    }

    public void setHint(@Nullable String hint) {
        this.hint = hint;
    }

    @Override
    public void drawTextBox() {
        super.drawTextBox();
        if ((hint == null || hint.isEmpty()) || !getText().isEmpty()) return;
        font.drawStringWithShadow(hint, x + 5, y + (height - 8) / 2f, 0x888888);
    }
}
