package com.tcc.pjb.backend.core.processo.policy.domain;

import java.util.List;
import java.util.Objects;

public record ProcessoPolicyDecision(
        String code,
        String blockCode,
        boolean active,
        String severity,
        String summary,
        String rationale,
        List<String> activeRules,
        List<String> deferredRules
) {
    public ProcessoPolicyDecision {
        Objects.requireNonNull(code);
        Objects.requireNonNull(blockCode);
        severity = severity == null ? "CONTROLADA" : severity;
        Objects.requireNonNull(summary);
        rationale = rationale == null ? "" : rationale;
        activeRules = activeRules == null ? List.of() : List.copyOf(activeRules);
        deferredRules = deferredRules == null ? List.of() : List.copyOf(deferredRules);
    }
}
