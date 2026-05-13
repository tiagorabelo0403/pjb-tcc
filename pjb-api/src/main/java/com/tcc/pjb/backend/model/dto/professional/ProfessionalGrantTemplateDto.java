package com.tcc.pjb.backend.model.dto.professional;

public record ProfessionalGrantTemplateDto(
        String templateCode,
        String label,
        String description,
        String actorClass,
        String grantType,
        String accessBasis,
        boolean requiresStepUp,
        boolean autoApproveAllowed,
        Integer defaultDurationDays,
        String targetMode,
        String defaultAnchor,
        String governanceTone
) {
}
