package com.tcc.pjb.backend.model.dto.professional;

import java.time.LocalDateTime;
import java.util.List;

public record ProfessionalGrantDetailResponse(
        LocalDateTime generatedAt,
        ProfessionalGrantQueueItemDto grant,
        List<ProfessionalGrantEventDto> timeline,
        List<ProfessionalForensicPanelLinkDto> routes,
        List<String> warnings
) {
}
