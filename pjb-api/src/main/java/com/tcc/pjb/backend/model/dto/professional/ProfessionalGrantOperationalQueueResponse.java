package com.tcc.pjb.backend.model.dto.professional;

import java.time.LocalDateTime;
import java.util.List;

public record ProfessionalGrantOperationalQueueResponse(
        LocalDateTime generatedAt,
        String actorClass,
        String actorLabel,
        List<ProfessionalGrantGovernanceFilterDto> filters,
        List<ProfessionalGrantGovernanceSummaryDto> summary,
        List<ProfessionalGrantQueueItemDto> criticalPending,
        List<ProfessionalGrantQueueItemDto> expiringImminent,
        List<ProfessionalGrantQueueItemDto> stepUpQueue,
        List<ProfessionalGrantTemplateDto> suggestedTemplates,
        List<ProfessionalForensicPanelLinkDto> routes,
        List<String> warnings
) {
}
