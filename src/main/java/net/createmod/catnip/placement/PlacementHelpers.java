package net.createmod.catnip.placement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PlacementHelpers {
    private static final List<IPlacementHelper> HELPERS = new ArrayList<IPlacementHelper>();

    private PlacementHelpers() {}

    public static synchronized int register(IPlacementHelper helper) {
        if (helper == null) {
            throw new IllegalArgumentException("helper");
        }
        HELPERS.add(helper);
        return HELPERS.size() - 1;
    }

    public static synchronized IPlacementHelper get(int id) {
        if (id < 0 || id >= HELPERS.size()) {
            throw new IndexOutOfBoundsException("Unknown placement helper " + id);
        }
        return HELPERS.get(id);
    }

    public static synchronized List<IPlacementHelper> getHelpersView() {
        return Collections.unmodifiableList(new ArrayList<IPlacementHelper>(HELPERS));
    }
}
