package com.tcc.pjb.backend.model.dto.professional;

import java.time.LocalDateTime;
import java.util.List;

public record ProfessionalGrantBatchOperationResponse(
        LocalDateTime generatedAt,
        String operation,
        String batchLabel,
        int requestedCount,
        int succeededCount,
        int failedCount,
        List<ProfessionalGrantQueueItemDto> processed,
        List<String> errors,
        List<ProfessionalForensicPanelLinkDto> routes
) {
}
