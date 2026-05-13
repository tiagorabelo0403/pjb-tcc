package com.tcc.pjb.backend.model.dto.julgamento.safety;

import java.time.Instant;

public record DecisionFocusResponse(
        Long sessionId,
        Long processoId,
        String numeroProcesso,
        String processFingerprint,
        String status,
        String resumoConfirmacao,
        String autor,
        String reu,
        String classeProcessual,
        String bindingFingerprint,
        Instant openedAt,
        Instant armedAt,
        Instant lastHeartbeatAt,
        Instant expiresAt
) {
}
