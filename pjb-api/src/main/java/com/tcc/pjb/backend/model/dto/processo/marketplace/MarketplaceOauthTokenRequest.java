package com.tcc.pjb.backend.model.dto.processo.marketplace;

public record MarketplaceOauthTokenRequest(
        String clientId,
        String clientSecret,
        String grantType,
        String scope
) {
}
