package net.createmod.ponder.config;

import net.createmod.catnip.config.ConfigBase;
import net.createmod.ponder.enums.PonderConfig;

public final class CClient extends ConfigBase {
    public final ConfigBool comfyReading = b(false, "comfyReading",
        "Slow down a Ponder scene while text is visible.");
    public final ConfigBool editingMode = b(false, "editingMode",
        "Reload registrations and display scene authoring information.");
    public final ConfigGroup placementAssist = group(1, "placementAssist",
        "Settings for assisted-placement indicators.");
    public final ConfigEnum<PlacementIndicatorSetting> placementIndicator =
        e(PlacementIndicatorSetting.TEXTURE, "indicatorType",
            "TEXTURE, TRIANGLE, or NONE.");
    public final ConfigFloat indicatorScale = f(1f, 0f, 8f, "indicatorScale",
        "Size multiplier for the assisted-placement indicator.");

    public enum PlacementIndicatorSetting {
        TEXTURE, TRIANGLE, NONE
    }

    @Override public String getName() { return "client"; }

    @Override public void onLoad() {
        super.onLoad();
        synchronizeFacade();
    }

    @Override public void onReload() {
        super.onReload();
        synchronizeFacade();
    }

    private void synchronizeFacade() {
        PonderConfig.client().setComfyReading(comfyReading.get());
        PonderConfig.client().setEditingMode(editingMode.get());
        PonderConfig.client().setIndicatorScale(indicatorScale.getF());
        PonderConfig.client().setPlacementIndicator(
            PonderConfig.PlacementIndicatorSetting.valueOf(placementIndicator.get().name()));
    }
}
