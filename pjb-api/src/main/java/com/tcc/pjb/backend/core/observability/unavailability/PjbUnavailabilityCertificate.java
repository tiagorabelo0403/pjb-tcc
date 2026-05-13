package com.tcc.pjb.backend.core.observability.unavailability;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record PjbUnavailabilityCertificate(
        String tribunalCode,
        Instant issuedAt,
        boolean deadlineImpactRecognized,
        String creditedOutage,
        List<String> affectedServices,
        List<String> reasons,
        String evidenceHash
) {
    public PjbUnavailabilityCertificate {
        tribunalCode = Objects.toString(tribunalCode, "").trim().toUpperCase();
        issuedAt = issuedAt == null ? Instant.EPOCH : issuedAt;
        creditedOutage = Objects.toString(creditedOutage, "PT0S").trim();
        affectedServices = affectedServices == null ? List.of() : List.copyOf(affectedServices);
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
        evidenceHash = Objects.toString(evidenceHash, "").trim();
    }
}
