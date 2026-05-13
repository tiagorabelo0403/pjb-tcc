package com.tcc.pjb.backend.model.dto.professional;

import java.time.LocalDateTime;
import java.util.List;

public record ProfessionalForensicInstitutionalOverviewResponse(
        LocalDateTime generatedAt,
        String actorClass,
        String panelMode,
        String actorLabel,
        String territorialAnchor,
        List<ProfessionalForensicInstitutionalFilterDto> filters,
        List<ProfessionalForensicInstitutionalMetricDto> metrics,
        List<ProfessionalForensicInstitutionalModuleDto> modules,
        List<ProfessionalForensicProcessCardDto> spotlightProcesses,
        List<ProfessionalRecentAuditDto> recentActivity,
        List<ProfessionalForensicPanelLinkDto> routes,
        List<String> warnings
) {
}
