package com.tcc.pjb.backend.model.dto.processual.painel.contextual;

import java.time.Instant;
import java.util.List;

public record ProcessoPainelConectorSaudeResponse(
        String systemCode,
        String status,
        String accentColor,
        double successRate,
        boolean submissionReady,
        boolean syncReady,
        Instant latestEventAt,
        List<String> blockers,
        List<String> warnings
) {
    public ProcessoPainelConectorSaudeResponse {
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
