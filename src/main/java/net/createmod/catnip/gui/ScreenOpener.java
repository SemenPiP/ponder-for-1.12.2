package net.createmod.catnip.gui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ScreenOpener {
    private static final Deque<GuiScreen> HISTORY = new ArrayDeque<GuiScreen>();
    private static boolean navigatingBack;

    private ScreenOpener() {}

    public static void open(@Nullable final GuiScreen screen) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.currentScreen != null && minecraft.currentScreen != screen && !navigatingBack) {
            HISTORY.push(minecraft.currentScreen);
        }
        minecraft.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                Minecraft.getMinecraft().displayGuiScreen(screen);
            }
        });
    }

    public static boolean openPreviousScreen() {
        if (HISTORY.isEmpty()) {
            return false;
        }
        navigatingBack = true;
        try {
            open(HISTORY.pop());
        } finally {
            navigatingBack = false;
        }
        return true;
    }

    public static List<GuiScreen> getScreenHistory() {
        return new ArrayList<GuiScreen>(HISTORY);
    }

    public static void clearHistory() {
        HISTORY.clear();
    }
}
