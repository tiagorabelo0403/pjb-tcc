package com.tcc.pjb.backend.model.dto.professional;

import java.time.LocalDateTime;
import java.util.List;

public record ProfessionalForensicAccessMatrixResponse(
        LocalDateTime generatedAt,
        String actorClass,
        String panelMode,
        Long processoId,
        String numero,
        String primaryBasis,
        String reason,
        String organizationalAnchor,
        boolean represented,
        boolean publicOnly,
        boolean requiresStepUp,
        long lineageCount,
        List<ProfessionalForensicAccessLineageDto> lineage,
        List<String> capabilityCodes,
        List<String> allowedScopes,
        List<String> restrictedScopes,
        List<ProfessionalForensicPanelLinkDto> routes,
        List<String> warnings
) {
}
