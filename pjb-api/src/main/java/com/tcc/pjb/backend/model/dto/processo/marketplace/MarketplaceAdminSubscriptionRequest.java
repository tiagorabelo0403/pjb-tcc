package com.tcc.pjb.backend.model.dto.processo.marketplace;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record MarketplaceAdminSubscriptionRequest(
        @NotBlank @Size(max = 80) String planCode,
        Instant startedAt,
        Instant endsAt,
        Integer webhookEndpointLimitOverride,
        @Size(max = 500) String observacoes
) {
}
