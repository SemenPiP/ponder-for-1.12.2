package net.createmod.ponder.enums;

/** Side-neutral values populated by the Forge Configuration adapter on the client. */
public final class PonderConfig {
    private static final Client CLIENT = new Client();

    private PonderConfig() {
    }

    public static Client client() {
        return CLIENT;
    }

    public enum PlacementIndicatorSetting {
        TEXTURE, TRIANGLE, NONE
    }

    public static final class Client {
        private volatile boolean comfyReading;
        private volatile boolean editingMode;
        private volatile PlacementIndicatorSetting placementIndicator = PlacementIndicatorSetting.TEXTURE;
        private volatile float indicatorScale = 1;

        public boolean isComfyReading() { return comfyReading; }
        public void setComfyReading(boolean value) { comfyReading = value; }
        public boolean isEditingMode() { return editingMode; }
        public void setEditingMode(boolean value) { editingMode = value; }
        public PlacementIndicatorSetting getPlacementIndicator() { return placementIndicator; }
        public void setPlacementIndicator(PlacementIndicatorSetting value) {
            placementIndicator = value == null ? PlacementIndicatorSetting.TEXTURE : value;
        }
        public float getIndicatorScale() { return indicatorScale; }
        public void setIndicatorScale(float value) {
            if (Float.isNaN(value) || Float.isInfinite(value)) throw new IllegalArgumentException("Invalid indicator scale");
            indicatorScale = Math.max(0, Math.min(8, value));
        }
    }
}
