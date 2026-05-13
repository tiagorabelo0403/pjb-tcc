package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalIntegrationCallTrailResponse(
        String trailId,
        String credentialId,
        String correlationId,
        String origin,
        String payloadDigest,
        boolean payloadSignaturePresent,
        String idempotencyKey,
        String resultCode,
        List<String> findings,
        Instant calledAt
) {
}
