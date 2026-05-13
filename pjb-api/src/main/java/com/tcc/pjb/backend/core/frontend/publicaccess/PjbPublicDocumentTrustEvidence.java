package com.tcc.pjb.backend.core.frontend.publicaccess;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record PjbPublicDocumentTrustEvidence(
        String documentCode,
        String expectedSha256,
        String actualSha256,
        boolean signatureValid,
        boolean timestampPresent,
        boolean publicVersionAvailable,
        boolean revoked,
        Instant verifiedAt,
        List<String> publicMessages
) {
    public PjbPublicDocumentTrustEvidence {
        documentCode = Objects.toString(documentCode, "").trim();
        expectedSha256 = Objects.toString(expectedSha256, "").trim();
        actualSha256 = Objects.toString(actualSha256, "").trim();
        verifiedAt = verifiedAt == null ? Instant.EPOCH : verifiedAt;
        publicMessages = publicMessages == null ? List.of() : List.copyOf(publicMessages);
    }
}
