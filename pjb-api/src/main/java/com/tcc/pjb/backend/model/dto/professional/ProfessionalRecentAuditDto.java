package com.tcc.pjb.backend.model.dto.professional;

import java.time.LocalDateTime;

public record ProfessionalRecentAuditDto(
        Long auditId,
        LocalDateTime accessedAt,
        String operationType,
        Long processoId,
        String numeroProcesso,
        String actorClass,
        String accessBasis,
        String queryType,
        String queryValueMasked,
        boolean success,
        String reason
) {
}
