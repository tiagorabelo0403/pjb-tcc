package com.tcc.pjb.backend.core.kernel.advisory;

import java.util.List;
import java.util.Map;

public record StrategicCopilotReport(
        String lane,
        String phase,
        double score,
        List<Action> immediateActions,
        List<Action> evidenceActions,
        List<Action> proceduralActions,
        List<Action> jurisprudentialActions,
        List<Action> negotiationActions,
        List<String> watchpoints,
        Map<String, Object> diagnostics
) {
    public record Action(
            String code,
            String title,
            String severity,
            String rationale,
            List<String> steps
    ) {
    }
}
