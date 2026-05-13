package com.tcc.pjb.backend.model.dto.professional;

import java.time.LocalDateTime;
import java.util.List;

public record ProfessionalForensicClient360Response(
        LocalDateTime generatedAt,
        String actorClass,
        String panelMode,
        String identityMode,
        String normalizedIdentity,
        String territorialBucket,
        long totalProcesses,
        long publicQualifiedCount,
        long representedCount,
        long confidentialEligibleCount,
        List<ProfessionalForensicClientBucketDto> buckets,
        List<ProfessionalForensicProcessCardDto> results,
        List<ProfessionalForensicPanelLinkDto> routes,
        List<String> warnings
) {
}
