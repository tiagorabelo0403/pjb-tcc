package com.tcc.pjb.backend.model.dto.professional;

import java.time.LocalDateTime;

public record ProfessionalGrantQueueItemDto(
        Long grantId,
        String approvalStatus,
        String approvalStatusLabel,
        String actorClass,
        String grantType,
        String grantTypeLabel,
        String accessBasis,
        String accessBasisLabel,
        Long targetUserId,
        String targetUserName,
        String targetProfessionalLabel,
        String processoNumero,
        String organizationalAnchor,
        boolean requiresStepUp,
        LocalDateTime requestedAt,
        String requestedByName,
        LocalDateTime approvedAt,
        String approvedByName,
        LocalDateTime revokedAt,
        String revokedByName,
        String reason,
        String tone
) {
}
