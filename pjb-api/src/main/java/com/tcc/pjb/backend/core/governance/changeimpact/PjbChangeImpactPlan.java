package com.tcc.pjb.backend.core.governance.changeimpact;

import java.util.List;

public record PjbChangeImpactPlan(
        String status,
        boolean safeToProceed,
        List<PjbChangeImpactSignal> signals,
        List<String> requiredGuards,
        List<String> rollbackActions
) {
    public PjbChangeImpactPlan {
        status = status == null || status.isBlank() ? "REVIEW_REQUIRED" : status.trim();
        signals = signals == null ? List.of() : List.copyOf(signals);
        requiredGuards = requiredGuards == null ? List.of() : List.copyOf(requiredGuards);
        rollbackActions = rollbackActions == null ? List.of() : List.copyOf(rollbackActions);
    }

    public boolean blocksRelease() {
        return !safeToProceed || signals.stream().anyMatch(PjbChangeImpactSignal::blocking);
    }
}
