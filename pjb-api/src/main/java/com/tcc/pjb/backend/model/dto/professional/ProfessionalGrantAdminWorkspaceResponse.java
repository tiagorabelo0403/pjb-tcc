package com.tcc.pjb.backend.model.dto.professional;

import java.time.LocalDateTime;
import java.util.List;

public record ProfessionalGrantAdminWorkspaceResponse(
        LocalDateTime generatedAt,
        String actorClass,
        String actorLabel,
        List<String> manageableGrantTypes,
        List<ProfessionalGrantQueueItemDto> pendingApprovals,
        List<ProfessionalGrantQueueItemDto> myRecentRequests,
        List<ProfessionalGrantQueueItemDto> activeProcessScopedGrants,
        List<ProfessionalForensicPanelLinkDto> routes,
        List<String> warnings
) {
}
