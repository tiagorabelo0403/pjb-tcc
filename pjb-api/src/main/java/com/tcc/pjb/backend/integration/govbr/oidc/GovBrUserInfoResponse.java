package com.tcc.pjb.backend.integration.govbr.oidc;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GovBrUserInfoResponse(
    @JsonProperty("sub") String sub,
    @JsonProperty("name") String name,
    @JsonProperty("social_name") String socialName,
    @JsonProperty("email") String email,
    @JsonProperty("email_verified") Boolean emailVerified,
    @JsonProperty("phone_number") String phoneNumber,
    @JsonProperty("phone_number_verified") Boolean phoneNumberVerified,
    @JsonProperty("picture") String picture
) {
}
