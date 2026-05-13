package com.tcc.pjb.backend.model.dto.processo.marketplace;

public record MarketplaceOauthRevocationRequest(
        String token,
        String motivo
) {
}
