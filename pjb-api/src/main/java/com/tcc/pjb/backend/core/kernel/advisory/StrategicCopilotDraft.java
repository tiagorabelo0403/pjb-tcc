package com.tcc.pjb.backend.core.kernel.advisory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

final class StrategicCopilotDraft {

    private final List<StrategicCopilotReport.Action> immediate = new ArrayList<>();
    private final List<StrategicCopilotReport.Action> evidence = new ArrayList<>();
    private final List<StrategicCopilotReport.Action> procedural = new ArrayList<>();
    private final List<StrategicCopilotReport.Action> jurisprudential = new ArrayList<>();
    private final List<StrategicCopilotReport.Action> negotiation = new ArrayList<>();
    private final LinkedHashSet<String> watchpoints = new LinkedHashSet<>();
    private double score;

    StrategicCopilotDraft(double initialScore) {
        this.score = initialScore;
    }

    void immediate(StrategicCopilotReport.Action action) {
        immediate.add(action);
    }

    void evidence(StrategicCopilotReport.Action action) {
        evidence.add(action);
    }

    void procedural(StrategicCopilotReport.Action action) {
        procedural.add(action);
    }

    void jurisprudential(StrategicCopilotReport.Action action) {
        jurisprudential.add(action);
    }

    void negotiation(StrategicCopilotReport.Action action) {
        negotiation.add(action);
    }

    void watchpoint(String watchpoint) {
        if (watchpoint != null && !watchpoint.isBlank()) {
            watchpoints.add(watchpoint.trim());
        }
    }

    void watchpoints(Iterable<String> items) {
        if (items == null) {
            return;
        }
        for (String item : items) {
            watchpoint(item);
        }
    }

    void score(double delta) {
        score += delta;
    }

    StrategicCopilotReport toReport(String lane,
                                    String phase,
                                    Map<String, Object> diagnostics,
                                    StrategicCopilotSupport support) {
        return new StrategicCopilotReport(
                lane,
                phase,
                support.round(support.clamp(score)),
                List.copyOf(immediate),
                List.copyOf(evidence),
                List.copyOf(procedural),
                List.copyOf(jurisprudential),
                List.copyOf(negotiation),
                List.copyOf(watchpoints),
                diagnostics == null ? Map.of() : Map.copyOf(diagnostics)
        );
    }
}
