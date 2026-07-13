package net.createmod.ponder.script;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ScriptSyncNotices {
    private static final Set<String> PENDING = new LinkedHashSet<String>();

    private ScriptSyncNotices() {
    }

    public static synchronized void record(String message) {
        if (message != null && !message.trim().isEmpty())
            PENDING.add(message);
    }

    public static synchronized List<String> drain() {
        List<String> result = new ArrayList<String>(PENDING);
        PENDING.clear();
        return result;
    }
}
