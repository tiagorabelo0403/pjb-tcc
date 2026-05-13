package com.tcc.pjb.backend.model.dto.institutional;

public record InstitutionalWorkbenchMetricResponse(
        String code,
        String label,
        String value,
        String trend,
        String severity
) {
}
