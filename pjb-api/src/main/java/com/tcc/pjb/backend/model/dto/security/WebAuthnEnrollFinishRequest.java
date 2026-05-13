package com.tcc.pjb.backend.model.dto.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class WebAuthnEnrollFinishRequest {

    @NotNull
    private Long sessionId;

    @NotBlank
    @Size(max = 80)
    private String alias;

    @NotBlank
    private String credentialJson;

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }

    public String getCredentialJson() { return credentialJson; }
    public void setCredentialJson(String credentialJson) { this.credentialJson = credentialJson; }
}
