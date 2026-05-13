package com.tcc.pjb.backend.core.processo.health;

import java.util.List;

public record PjbProcessHealthSnapshot(
        String status,
        double score,
        boolean blocking,
        List<PjbProcessHealthSignal> signals,
        List<String> recommendedActions
) {
    public PjbProcessHealthSnapshot {
        status = status == null || status.isBlank() ? "WATCHLIST" : status.trim();
        score = Math.max(0.0d, Math.min(1.0d, score));
        signals = signals == null ? List.of() : List.copyOf(signals);
        recommendedActions = recommendedActions == null ? List.of() : List.copyOf(recommendedActions);
    }
}
