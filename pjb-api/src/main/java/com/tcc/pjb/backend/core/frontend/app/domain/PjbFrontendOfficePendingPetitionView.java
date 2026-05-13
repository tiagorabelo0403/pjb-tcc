package com.tcc.pjb.backend.core.frontend.app.domain;

import java.time.LocalDateTime;

public record PjbFrontendOfficePendingPetitionView(
        Long operationId,
        Long processoId,
        String numeroProcesso,
        String actionType,
        String status,
        Long queueItemId,
        Long executorUserId,
        Long signerUserId,
        String signerNome,
        String signerRegistration,
        boolean queueRequired,
        String accentColor,
        LocalDateTime createdAt,
        String readingModeRoute
) {
}
