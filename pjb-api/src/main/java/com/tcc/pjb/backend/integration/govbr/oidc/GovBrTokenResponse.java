package com.tcc.pjb.backend.integration.govbr.oidc;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GovBrTokenResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("id_token") String idToken,
    @JsonProperty("token_type") String tokenType,
    @JsonProperty("expires_in") long expiresIn,
    @JsonProperty("scope") String scope
) {
}
