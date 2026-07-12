package net.createmod.catnip.gui.element;

import net.createmod.catnip.render.GlStateGuard;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class GuiGameElement {
    private GuiGameElement() {}

    public static GuiRenderBuilder of(ItemStack stack) {
        return new GuiRenderBuilder(stack == null ? ItemStack.EMPTY : stack.copy());
    }

    public static GuiRenderBuilder of(Item item) {
        return of(new ItemStack(item));
    }

    public static GuiRenderBuilder of(Block block) {
        return of(new ItemStack(block));
    }

    public static GuiRenderBuilder of(IBlockState state) {
        return of(new ItemStack(state.getBlock(), 1, state.getBlock().getMetaFromState(state)));
    }

    public static final class GuiRenderBuilder implements ScreenElement {
        private final ItemStack stack;
        private float scale = 1f;
        private int offsetX;
        private int offsetY;

        private GuiRenderBuilder(ItemStack stack) {
            this.stack = stack;
        }

        public GuiRenderBuilder scale(float scale) {
            this.scale = scale;
            return this;
        }

        public GuiRenderBuilder atLocal(int x, int y) {
            offsetX = x;
            offsetY = y;
            return this;
        }

        @Override
        public void render(int x, int y) {
            render(x, y, 1f);
        }

        @Override
        public void render(int x, int y, float alpha) {
            if (stack.isEmpty()) {
                return;
            }
            try (GlStateGuard ignored = GlStateGuard.capture()) {
                GlStateManager.enableDepth();
                GlStateManager.enableRescaleNormal();
                GlStateManager.enableBlend();
                GlStateManager.color(1f, 1f, 1f, Math.max(0, Math.min(1, alpha)));
                RenderHelper.enableGUIStandardItemLighting();
                GlStateManager.pushMatrix();
                GlStateManager.translate(x + offsetX, y + offsetY, 0);
                GlStateManager.scale(scale, scale, scale);
                Minecraft.getMinecraft().getRenderItem().renderItemAndEffectIntoGUI(stack, -8, -8);
                GlStateManager.popMatrix();
            }
        }
    }
}
