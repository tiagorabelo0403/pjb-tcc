package com.tcc.pjb.backend.core.processo.painel.domain;

import java.time.Instant;
import java.util.List;

public record ProcessoPainelConectorSaude(
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
    public ProcessoPainelConectorSaude {
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
