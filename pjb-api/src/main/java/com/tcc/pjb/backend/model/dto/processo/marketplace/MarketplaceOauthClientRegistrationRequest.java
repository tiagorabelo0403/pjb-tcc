package com.tcc.pjb.backend.model.dto.processo.marketplace;

import java.util.List;

public record MarketplaceOauthClientRegistrationRequest(
        String clientId,
        String displayName,
        String ownerName,
        String ownerEmail,
        List<String> allowedScopes,
        String trustedOrigin,
        Integer accessTokenTtlSeconds
) {
}
