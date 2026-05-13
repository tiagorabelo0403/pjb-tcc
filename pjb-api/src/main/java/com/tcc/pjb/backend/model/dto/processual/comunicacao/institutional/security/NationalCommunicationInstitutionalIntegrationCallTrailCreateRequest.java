package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security;

import java.util.List;

public record NationalCommunicationInstitutionalIntegrationCallTrailCreateRequest(
        String correlationId,
        String origin,
        String payloadDigest,
        Boolean payloadSignaturePresent,
        String idempotencyKey,
        String resultCode,
        List<String> findings
) {
}
