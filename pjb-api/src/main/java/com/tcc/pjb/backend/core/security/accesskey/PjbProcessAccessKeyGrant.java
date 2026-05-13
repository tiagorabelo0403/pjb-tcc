package com.tcc.pjb.backend.core.security.accesskey;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

public record PjbProcessAccessKeyGrant(
        String processNumber,
        String keyFingerprint,
        Set<PjbProcessAccessKeyScope> scopes,
        Instant issuedAt,
        Instant expiresAt,
        boolean revoked,
        boolean sealedCase,
        String holderReference
) {
    public PjbProcessAccessKeyGrant {
        processNumber = Objects.toString(processNumber, "").trim();
        keyFingerprint = Objects.toString(keyFingerprint, "").trim();
        scopes = scopes == null ? Set.of() : Set.copyOf(scopes);
        issuedAt = issuedAt == null ? Instant.EPOCH : issuedAt;
        expiresAt = expiresAt == null ? Instant.EPOCH : expiresAt;
        holderReference = Objects.toString(holderReference, "").trim();
    }

    public boolean expiredAt(Instant instant) {
        Instant reference = instant == null ? Instant.EPOCH : instant;
        return !expiresAt.isAfter(reference);
    }

    public boolean hasScope(PjbProcessAccessKeyScope scope) {
        return scope != null && scopes.contains(scope);
    }
}
