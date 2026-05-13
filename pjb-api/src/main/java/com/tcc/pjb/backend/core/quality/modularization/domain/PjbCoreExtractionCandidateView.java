package com.tcc.pjb.backend.core.quality.modularization.domain;

import java.util.List;

public record PjbCoreExtractionCandidateView(
        String packageName,
        String moduleTarget,
        int fileCount,
        String risk,
        List<String> notes
) {
    public PjbCoreExtractionCandidateView {
        packageName = packageName == null ? "" : packageName;
        moduleTarget = moduleTarget == null ? "pjb-core" : moduleTarget;
        risk = risk == null ? "DESCONHECIDO" : risk;
        notes = notes == null ? List.of() : List.copyOf(notes);
    }
}
