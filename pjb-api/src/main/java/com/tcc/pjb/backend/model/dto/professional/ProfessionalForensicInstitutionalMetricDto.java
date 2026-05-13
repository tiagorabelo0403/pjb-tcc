package com.tcc.pjb.backend.model.dto.professional;

public record ProfessionalForensicInstitutionalMetricDto(
        String code,
        String label,
        long value,
        String accent,
        String summary
) {
}
