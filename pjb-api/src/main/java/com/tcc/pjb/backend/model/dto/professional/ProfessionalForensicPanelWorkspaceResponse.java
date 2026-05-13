package com.tcc.pjb.backend.model.dto.professional;

import java.time.LocalDateTime;
import java.util.List;

public record ProfessionalForensicPanelWorkspaceResponse(
        LocalDateTime generatedAt,
        String panelMode,
        String actorClass,
        String actorLabel,
        String accessHeadline,
        List<String> searchModes,
        List<String> capabilityCodes,
        List<String> professionalDifferentials,
        List<ProfessionalRecentAuditDto> recentActivity,
        List<ProfessionalForensicPanelLinkDto> routes
) {
}
