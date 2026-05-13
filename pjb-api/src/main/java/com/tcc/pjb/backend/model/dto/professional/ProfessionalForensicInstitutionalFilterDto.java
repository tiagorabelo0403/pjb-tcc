package com.tcc.pjb.backend.model.dto.professional;

public record ProfessionalForensicInstitutionalFilterDto(
        String code,
        String label,
        String value,
        boolean locked,
        String rationale
) {
}
