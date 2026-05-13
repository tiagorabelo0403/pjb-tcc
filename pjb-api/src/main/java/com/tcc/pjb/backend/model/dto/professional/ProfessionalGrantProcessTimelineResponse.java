package com.tcc.pjb.backend.model.dto.professional;

import java.time.LocalDateTime;
import java.util.List;

public record ProfessionalGrantProcessTimelineResponse(
        LocalDateTime generatedAt,
        String processoNumero,
        List<ProfessionalGrantQueueItemDto> grants,
        List<ProfessionalGrantEventDto> events,
        List<ProfessionalForensicPanelLinkDto> routes,
        List<String> warnings
) {
}
