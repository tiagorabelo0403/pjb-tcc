package com.tcc.pjb.backend.model.dto.processo.marketplace;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MarketplaceAdminPlanRequest(
        @NotBlank @Size(max = 80) String code,
        @NotBlank @Size(max = 140) String displayName,
        @Min(1) int maxProtocolosDia,
        @Min(1) int maxWebhookEndpoints,
        boolean allowStreaming,
        boolean allowHighVolume,
        @Size(max = 500) String description
) {
}
