package com.tcc.pjb.backend.model.dto.professional;

public record ProfessionalForensicAccessLineageDto(
        String code,
        String title,
        String basis,
        String sourceLabel,
        String territorialAnchor,
        boolean processBound,
        boolean requiresStepUp,
        String validUntil,
        String tone
) {
}
