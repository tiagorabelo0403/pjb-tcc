package com.tcc.pjb.backend.model.dto.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PasskeyFinishRequest {

    @NotNull
    private Long sessionId;

    @NotBlank
    private String credentialJson;

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public String getCredentialJson() { return credentialJson; }
    public void setCredentialJson(String credentialJson) { this.credentialJson = credentialJson; }
}
