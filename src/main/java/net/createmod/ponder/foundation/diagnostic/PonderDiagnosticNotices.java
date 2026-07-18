package net.createmod.ponder.foundation.diagnostic;

import java.util.LinkedHashSet;
import java.util.Set;

import net.createmod.ponder.Ponder;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticIssue;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticSeverity;

public final class PonderDiagnosticNotices {
    private static final Set<String> PENDING = new LinkedHashSet<String>();
    private static final Set<String> SEEN = new LinkedHashSet<String>();
    private static long generation = Long.MIN_VALUE;

    private PonderDiagnosticNotices() {
    }

    public static synchronized void beginGeneration(long nextGeneration) {
        if (generation == nextGeneration)
            return;
        generation = nextGeneration;
        PENDING.clear();
        SEEN.clear();
    }

    public static synchronized void record(PonderDiagnosticIssue issue) {
        if (issue == null)
            return;
        String key = key(issue);
        if (!SEEN.add(key))
            return;
        PENDING.add(key);
        String context = issue.getSceneId() == null ? ""
            : " scene=" + issue.getSceneId();
        if (issue.hasInstructionIndex())
            context += " instruction=" + issue.getInstructionIndex();
        if (issue.getSeverity() == PonderDiagnosticSeverity.ERROR)
            Ponder.LOGGER.error("{}{}: {}", issue.getCode(), context, issue.getMessage());
        else if (issue.getSeverity() == PonderDiagnosticSeverity.WARNING)
            Ponder.LOGGER.warn("{}{}: {}", issue.getCode(), context, issue.getMessage());
        else
            Ponder.LOGGER.info("{}{}: {}", issue.getCode(), context, issue.getMessage());
    }

    public static synchronized String drainSummary() {
        if (PENDING.isEmpty())
            return null;
        int errors = 0;
        int warnings = 0;
        for (String value : PENDING) {
            if (value.startsWith("ERROR|"))
                errors++;
            else if (value.startsWith("WARNING|"))
                warnings++;
        }
        int total = PENDING.size();
        PENDING.clear();
        return "Ponder detected " + total + " diagnostic issue(s) (" + errors + " error(s), "
            + warnings + " warning(s)). Run /ponder validate for details.";
    }

    private static String key(PonderDiagnosticIssue issue) {
        return issue.getSeverity() + "|" + issue.getCode() + "|" + issue.getMessage() + "|"
            + issue.getSceneId() + "|" + issue.getInstructionIndex();
    }
}
