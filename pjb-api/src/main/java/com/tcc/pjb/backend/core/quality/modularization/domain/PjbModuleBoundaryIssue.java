package com.tcc.pjb.backend.core.quality.modularization.domain;

import java.util.List;

public record PjbModuleBoundaryIssue(
        String code,
        String severity,
        String location,
        String summary,
        List<String> details
) {
    public PjbModuleBoundaryIssue {
        details = details == null ? List.of() : List.copyOf(details);
    }
}
