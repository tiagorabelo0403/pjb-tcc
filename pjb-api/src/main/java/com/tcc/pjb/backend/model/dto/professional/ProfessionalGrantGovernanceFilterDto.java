package com.tcc.pjb.backend.model.dto.professional;

public record ProfessionalGrantGovernanceFilterDto(
        String code,
        String label,
        String value,
        boolean applied,
        String hint
) {
}
