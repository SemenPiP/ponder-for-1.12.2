package net.createmod.catnip.config.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Scrollable Forge-era list of loaded mods and their Catnip configuration availability. */
@SideOnly(Side.CLIENT)
public class ConfigScreenList extends GuiSlot {
    private final GuiScreen parent;
    private final List<ModEntry> allEntries = new ArrayList<ModEntry>();
    private final List<ModEntry> visibleEntries = new ArrayList<ModEntry>();

    public ConfigScreenList(Minecraft minecraft, GuiScreen parent, int width, int height,
                            int top, int bottom, int slotHeight) {
        super(minecraft, width, height, top, bottom, slotHeight);
        this.parent = parent;
        setShowSelectionBox(true);
        reload();
    }

    public final void reload() {
        allEntries.clear();
        for (ModContainer mod : Loader.instance().getActiveModList()) {
            allEntries.add(new ModEntry(mod.getModId(), mod.getName(),
                ConfigHelper.hasAnyConfig(mod.getModId())));
        }
        Collections.sort(allEntries, new Comparator<ModEntry>() {
            @Override public int compare(ModEntry left, ModEntry right) {
                if (left.hasConfig != right.hasConfig) return left.hasConfig ? -1 : 1;
                return left.displayName.compareToIgnoreCase(right.displayName);
            }
        });
        setFilter("");
    }

    public void setFilter(String filter) {
        String query = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        visibleEntries.clear();
        for (ModEntry entry : allEntries) {
            if (query.isEmpty() || entry.modId.toLowerCase(Locale.ROOT).contains(query)
                || entry.displayName.toLowerCase(Locale.ROOT).contains(query)) visibleEntries.add(entry);
        }
        selectedElement = -1;
        amountScrolled = 0;
    }

    public List<ModEntry> getVisibleEntries() {
        return Collections.unmodifiableList(visibleEntries);
    }

    @Override protected int getSize() { return visibleEntries.size(); }

    @Override
    protected void elementClicked(int slotIndex, boolean isDoubleClick, int mouseX, int mouseY) {
        if (slotIndex < 0 || slotIndex >= visibleEntries.size()) return;
        selectedElement = slotIndex;
        ModEntry entry = visibleEntries.get(slotIndex);
        if (entry.hasConfig) mc.displayGuiScreen(new BaseConfigScreen(parent, entry.modId));
    }

    @Override protected boolean isSelected(int slotIndex) { return slotIndex == selectedElement; }
    @Override protected void drawBackground() {
        Gui.drawRect(left, top, right, bottom, 0x66000000);
    }

    @Override
    protected void drawSlot(int slotIndex, int x, int y, int height, int mouseX, int mouseY,
                            float partialTicks) {
        ModEntry entry = visibleEntries.get(slotIndex);
        int color = entry.hasConfig ? 0xffffff : 0x777777;
        mc.fontRenderer.drawString(entry.displayName, x + 4, y + 3, color);
        mc.fontRenderer.drawString(entry.modId, x + 4, y + 13, entry.hasConfig ? 0xaaaaaa : 0x555555);
        String status = entry.hasConfig ? "Config" : "Unavailable";
        mc.fontRenderer.drawString(status, x + getListWidth() - mc.fontRenderer.getStringWidth(status) - 8,
            y + 8, entry.hasConfig ? 0x55ff55 : 0x777777);
    }

    @Override public int getListWidth() { return Math.min(360, width - 40); }

    public static final class ModEntry {
        public final String modId;
        public final String displayName;
        public final boolean hasConfig;
        private ModEntry(String modId, String displayName, boolean hasConfig) {
            this.modId = modId;
            this.displayName = displayName == null || displayName.trim().isEmpty() ? modId : displayName;
            this.hasConfig = hasConfig;
        }
    }
}
