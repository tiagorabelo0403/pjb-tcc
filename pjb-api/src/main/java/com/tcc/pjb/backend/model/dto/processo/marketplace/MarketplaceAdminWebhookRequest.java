package com.tcc.pjb.backend.model.dto.processo.marketplace;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MarketplaceAdminWebhookRequest(
        @NotBlank @Size(max = 320) String callbackUrl,
        @NotBlank @Size(max = 260) String eventFilter,
        @NotBlank @Size(min = 16, max = 256) String signingSecret
) {
}
