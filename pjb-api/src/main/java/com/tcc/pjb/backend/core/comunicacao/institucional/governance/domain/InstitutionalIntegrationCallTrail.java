package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

import com.tcc.pjb.backend.core.util.Hashes;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record InstitutionalIntegrationCallTrail(
        String trailId,
        String credentialId,
        String correlationId,
        String origin,
        String payloadDigest,
        boolean payloadSignaturePresent,
        String idempotencyKey,
        String resultCode,
        List<String> findings,
        Instant calledAt,
        String hashIntegridade
) {
    public InstitutionalIntegrationCallTrail {
        Objects.requireNonNull(trailId);
        Objects.requireNonNull(credentialId);
        findings = findings == null ? List.of() : List.copyOf(findings);
        if (hashIntegridade == null || hashIntegridade.isBlank()) {
            hashIntegridade = computeHash(trailId, credentialId, correlationId, origin, payloadDigest, payloadSignaturePresent, idempotencyKey, resultCode, findings, calledAt);
        }
    }

    private static String computeHash(Object... values) {
        StringBuilder sb = new StringBuilder("institutional_integration_call_trail");
        for (Object value : values) {
            sb.append('|').append(value == null ? '-' : value.toString());
        }
        return Hashes.sha256Hex(sb.toString());
    }
}
