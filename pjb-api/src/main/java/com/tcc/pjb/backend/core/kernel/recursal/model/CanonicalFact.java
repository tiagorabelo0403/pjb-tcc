package com.tcc.pjb.backend.core.kernel.recursal.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import com.tcc.pjb.backend.core.kernel.recursal.LegalIntegrationSystem;
import com.tcc.pjb.backend.core.kernel.recursal.RecursalFactType;
import com.tcc.pjb.backend.core.util.DeterministicUuid;

public record CanonicalFact(
        UUID factId,
        RecursalFactType type,
        LegalIntegrationSystem sourceSystem,
        String externalId,
        String sourceProceedingNumber,
        CanonicalFactPayload payload,
        Instant observedAt
) {

    public CanonicalFact {
        Objects.requireNonNull(type, "type é obrigatório");
        Objects.requireNonNull(sourceSystem, "sourceSystem é obrigatório");
        Objects.requireNonNull(sourceProceedingNumber, "sourceProceedingNumber é obrigatório");
        Objects.requireNonNull(payload, "payload é obrigatório");
        if (observedAt == null) observedAt = Instant.now();

        String ext = Objects.toString(externalId, "").trim();
        if (ext.isBlank()) {

            ext = type.name() + ":" + sourceProceedingNumber;
        }
        externalId = ext;

        if (factId == null) {
            factId = DeterministicUuid.v5("recursal.fact", sourceSystem.name() + "|" + externalId);
        }
    }


    public String dedupKey() {
        return externalId;
    }
}
