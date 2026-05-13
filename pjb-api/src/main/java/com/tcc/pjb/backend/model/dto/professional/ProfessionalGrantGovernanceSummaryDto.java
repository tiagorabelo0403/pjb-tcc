package com.tcc.pjb.backend.model.dto.professional;

public record ProfessionalGrantGovernanceSummaryDto(
        String code,
        String label,
        long total,
        String tone
) {
}
