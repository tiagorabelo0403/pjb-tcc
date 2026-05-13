package com.tcc.pjb.backend.model.dto.professional;

import java.time.LocalDateTime;
import java.util.List;

public record ProfessionalGrantGovernanceWorkspaceResponse(
        LocalDateTime generatedAt,
        String actorClass,
        String actorLabel,
        List<ProfessionalGrantGovernanceFilterDto> filters,
        List<ProfessionalGrantGovernanceSummaryDto> summary,
        List<ProfessionalGrantQueueItemDto> pendingApprovals,
        List<ProfessionalGrantQueueItemDto> expiringSoon,
        List<ProfessionalGrantQueueItemDto> filteredGrants,
        List<ProfessionalForensicPanelLinkDto> routes,
        List<String> warnings
) {
}
