package net.createmod.ponder.foundation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.platform.CatnipClientServices;
import net.createmod.ponder.api.subject.PonderSubjectResolvers;
import net.createmod.ponder.api.subject.ResolvedPonderSubject;
import net.createmod.ponder.enums.PonderKeybinds;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

public final class PonderTooltipHandler {
    private static final List<Consumer<ItemStack>> CALLBACKS = new ArrayList<Consumer<ItemStack>>();
    private static ItemStack tracked = ItemStack.EMPTY;
    private static ResourceLocation trackedComponent;
    private static long lastSeen;
    private static float progress;

    private PonderTooltipHandler() {
    }

    public static void onTooltip(ItemTooltipEvent event) {
        if (event.getEntityPlayer() == null || event.getItemStack().isEmpty()) return;

        ResolvedPonderSubject subject = PonderSubjectResolvers.resolve(event.getItemStack());
        if (!subject.isHandled()) return;
        ResourceLocation component = subject.getComponent();
        boolean scenesExist = PonderIndex.getSceneAccess().doScenesExistForId(component);
        if (!scenesExist) {
            if (shouldShowMissingSceneMessage(subject, false))
                event.getToolTip().add(TextFormatting.GRAY + I18n.format("ponder.ui.no_scenes_configured"));
            return;
        }

        if (!isTrackedSubject(event.getItemStack(), component)) {
            tracked = event.getItemStack().copy();
            trackedComponent = component;
            progress = 0;
        }
        lastSeen = System.currentTimeMillis();
        for (Consumer<ItemStack> callback : CALLBACKS) callback.accept(tracked.copy());

        String keyName = PonderKeybinds.PONDER.message().getUnformattedText();
        event.getToolTip().add(TextFormatting.GRAY + I18n.format("ponder.ui.hold_to_ponder", keyName));
        event.getToolTip().add(progressBar(progress));
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getMinecraft();
        boolean fresh = !tracked.isEmpty() && System.currentTimeMillis() - lastSeen < 180;
        boolean held = fresh && minecraft.currentScreen != null
            && CatnipClientServices.CLIENT_HOOKS.isKeyPressed(PonderKeybinds.PONDER.getMapping());
        progress = advanceProgress(progress, held);

        if (progress >= 1) {
            ResourceLocation opening = trackedComponent;
            progress = 0;
            clearTrackedSubject();
            try {
                if (opening != null) ScreenOpener.open(PonderUI.of(opening));
            } catch (RuntimeException ignored) {
            }
        }
        if (!fresh && progress == 0) clearTrackedSubject();
    }

    static boolean shouldShowMissingSceneMessage(ResolvedPonderSubject subject, boolean scenesExist) {
        return subject != null && subject.isHandled() && !subject.isDefaultResolver() && !scenesExist;
    }

    static boolean isSameSubject(ItemStack first, ResourceLocation firstComponent,
                                 ItemStack second, ResourceLocation secondComponent) {
        return first != null && second != null && !first.isEmpty() && !second.isEmpty()
            && firstComponent != null && firstComponent.equals(secondComponent)
            && first.getItem() == second.getItem()
            && first.getMetadata() == second.getMetadata()
            && ItemStack.areItemStackTagsEqual(first, second);
    }

    private static boolean isTrackedSubject(ItemStack stack, ResourceLocation component) {
        return isSameSubject(tracked, trackedComponent, stack, component);
    }

    private static void clearTrackedSubject() {
        tracked = ItemStack.EMPTY;
        trackedComponent = null;
    }

    static float advanceProgress(float current, boolean held) {
        return Math.max(0, Math.min(1, current + (held ? .09f : -.12f)));
    }

    static String progressBar(float value) {
        int filled = Math.round(Math.max(0, Math.min(1, value)) * 10);
        StringBuilder bar = new StringBuilder(TextFormatting.DARK_GRAY.toString()).append('[');
        for (int i = 0; i < 10; i++) {
            bar.append(i < filled
                ? TextFormatting.AQUA.toString() + "|"
                : TextFormatting.DARK_GRAY.toString() + "|");
        }
        return bar.append(TextFormatting.DARK_GRAY).append(']').toString();
    }

    public static float getProgress() {
        return progress;
    }

    public static synchronized void registerHoveredPonderStackCallback(Consumer<ItemStack> callback) {
        if (callback != null) CALLBACKS.add(callback);
    }

    public static synchronized void removeHoveredPonderStackCallback(Consumer<ItemStack> callback) {
        CALLBACKS.remove(callback);
    }
}
