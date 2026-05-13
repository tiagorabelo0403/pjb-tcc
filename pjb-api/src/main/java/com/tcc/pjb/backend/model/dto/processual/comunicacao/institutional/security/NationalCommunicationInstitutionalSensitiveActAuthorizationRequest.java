package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security;

public record NationalCommunicationInstitutionalSensitiveActAuthorizationRequest(
        String sensitiveAct,
        String affiliationId,
        String nominationId
) {
}
