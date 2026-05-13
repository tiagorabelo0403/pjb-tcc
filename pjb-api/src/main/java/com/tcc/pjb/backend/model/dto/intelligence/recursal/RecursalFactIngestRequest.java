package com.tcc.pjb.backend.model.dto.intelligence.recursal;

import java.time.Instant;
import jakarta.validation.constraints.NotNull;
import com.tcc.pjb.backend.core.kernel.recursal.LegalIntegrationSystem;
import com.tcc.pjb.backend.core.kernel.recursal.RecursalFactType;
import com.tcc.pjb.backend.core.kernel.recursal.model.CanonicalFactPayload;

public record RecursalFactIngestRequest(
        @NotNull RecursalFactType type,
        LegalIntegrationSystem sourceSystem,
        String externalId,
        String sourceProceedingNumber,
        @NotNull CanonicalFactPayload payload,
        Instant observedAt
) {

    public RecursalFactIngestRequest {
        if (sourceSystem == null) sourceSystem = LegalIntegrationSystem.MANUAL;
    }
}
