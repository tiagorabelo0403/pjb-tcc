package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalIntegrationCredentialStatus;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record InstitutionalIntegrationCredential(
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
        InstitutionalIntegrationCredentialStatus status,
        String keyId,
        String secretHash,
        String secretPreview,
        Instant issuedAt,
        Instant rotatedAt,
        Instant expiresAt,
        Instant revokedAt,
        List<String> fundamentos,
        String hashIntegridade
) {
    public InstitutionalIntegrationCredential {
        Objects.requireNonNull(credentialId);
        Objects.requireNonNull(affiliationId);
        Objects.requireNonNull(displayName);
        Objects.requireNonNull(status);
        integrationFamilies = integrationFamilies == null ? List.of() : List.copyOf(integrationFamilies);
        originAllowlist = originAllowlist == null ? List.of() : List.copyOf(originAllowlist);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        if (hashIntegridade == null || hashIntegridade.isBlank()) {
            hashIntegridade = computeHash(credentialId, affiliationId, displayName, integrationFamilies, originAllowlist, requiresPayloadSignature,
                    requiresMutualTls, requiresHumanApproval, requiresImmediateRevocation, credentialRotationDays, status, keyId, secretHash,
                    secretPreview, issuedAt, rotatedAt, expiresAt, revokedAt, fundamentos);
        }
    }

    public boolean ativaEm(Instant instant) {
        Instant ref = instant == null ? Instant.now() : instant;
        return status.isAtiva() && (expiresAt == null || expiresAt.isAfter(ref)) && revokedAt == null;
    }

    public InstitutionalIntegrationCredential withRotation(String nextHash,
                                                           String nextPreview,
                                                           Instant now,
                                                           List<String> extraFundamentos) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>(fundamentos);
        if (extraFundamentos != null) {
            out.addAll(extraFundamentos);
        }
        return new InstitutionalIntegrationCredential(
                credentialId,
                affiliationId,
                displayName,
                integrationFamilies,
                originAllowlist,
                requiresPayloadSignature,
                requiresMutualTls,
                requiresHumanApproval,
                requiresImmediateRevocation,
                credentialRotationDays,
                InstitutionalIntegrationCredentialStatus.ROTACIONADA,
                keyId,
                nextHash,
                nextPreview,
                issuedAt,
                now,
                now.plusSeconds(credentialRotationDays * 86400L),
                null,
                List.copyOf(out.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).distinct().toList()),
                null
        );
    }

    public InstitutionalIntegrationCredential revoke(Instant now, List<String> extraFundamentos) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>(fundamentos);
        if (extraFundamentos != null) {
            out.addAll(extraFundamentos);
        }
        return new InstitutionalIntegrationCredential(
                credentialId,
                affiliationId,
                displayName,
                integrationFamilies,
                originAllowlist,
                requiresPayloadSignature,
                requiresMutualTls,
                requiresHumanApproval,
                requiresImmediateRevocation,
                credentialRotationDays,
                InstitutionalIntegrationCredentialStatus.REVOGADA,
                keyId,
                secretHash,
                secretPreview,
                issuedAt,
                rotatedAt,
                expiresAt,
                now,
                List.copyOf(out.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).distinct().toList()),
                null
        );
    }

    private static String computeHash(Object... values) {
        StringBuilder sb = new StringBuilder("institutional_integration_credential");
        for (Object value : values) {
            sb.append('|').append(value == null ? '-' : value.toString());
        }
        return Hashes.sha256Hex(sb.toString());
    }
}
