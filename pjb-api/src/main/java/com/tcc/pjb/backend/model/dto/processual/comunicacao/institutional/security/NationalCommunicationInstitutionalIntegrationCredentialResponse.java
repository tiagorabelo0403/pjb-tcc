package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalIntegrationCredentialResponse(
        String credentialId,
        String affiliationId,
        String displayName,
        List<String> integrationFamilies,
        List<String> originAllowlist,
        boolean requiresPayloadSignature,
        boolean requiresMutualTls,
        boolean requiresHumanApproval,
        boolean requiresImmediateRevocation,
        int credentialRotationDays,
        String status,
        String keyId,
        String secretPreview,
        String plaintextSecret,
        Instant issuedAt,
        Instant rotatedAt,
        Instant expiresAt,
        Instant revokedAt,
        List<String> fundamentos
) {
}
