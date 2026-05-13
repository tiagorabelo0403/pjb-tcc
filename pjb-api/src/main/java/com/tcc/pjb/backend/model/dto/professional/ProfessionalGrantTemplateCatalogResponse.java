package com.tcc.pjb.backend.model.dto.professional;

import java.time.LocalDateTime;
import java.util.List;

public record ProfessionalGrantTemplateCatalogResponse(
        LocalDateTime generatedAt,
        String actorClass,
        String actorLabel,
        List<ProfessionalGrantTemplateDto> templates,
        List<ProfessionalForensicPanelLinkDto> routes,
        List<String> warnings
) {
}
