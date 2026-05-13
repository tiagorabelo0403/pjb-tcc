package com.tcc.pjb.backend.core.quality.modularization.domain;

import java.util.List;

public record PjbModuleBoundaryPhaseView(
        int phase,
        String title,
        String status,
        String goal,
        List<String> blockers
) {
    public PjbModuleBoundaryPhaseView {
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
    }
}
