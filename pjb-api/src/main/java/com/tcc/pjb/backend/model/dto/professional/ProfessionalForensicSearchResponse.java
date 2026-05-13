package com.tcc.pjb.backend.model.dto.professional;

import java.time.LocalDateTime;
import java.util.List;

public record ProfessionalForensicSearchResponse(
        LocalDateTime generatedAt,
        String actorClass,
        String panelMode,
        String normalizedQuery,
        int page,
        int size,
        long total,
        List<String> regionBuckets,
        List<ProfessionalForensicProcessCardDto> results,
        List<String> warnings
) {
}
