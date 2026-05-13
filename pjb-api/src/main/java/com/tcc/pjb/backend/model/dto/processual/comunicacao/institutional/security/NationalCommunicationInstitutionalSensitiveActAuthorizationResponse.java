package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalSensitiveActAuthorizationResponse(
        String authorizationId,
        String sensitiveAct,
        Long userId,
        String userName,
        String affiliationId,
        String nominationId,
        String achievedTrust,
        String requiredTrust,
        boolean allowed,
        boolean requiresManualApproval,
        boolean blocked,
        List<String> findings,
        List<String> fundamentos,
        Instant evaluatedAt
) {
}
