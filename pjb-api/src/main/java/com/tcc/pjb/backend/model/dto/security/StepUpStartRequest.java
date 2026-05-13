package com.tcc.pjb.backend.model.dto.security;

import jakarta.validation.constraints.NotBlank;

public class StepUpStartRequest {

    @NotBlank
    private String action;

    @NotBlank
    private String requestHash;

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getRequestHash() { return requestHash; }
    public void setRequestHash(String requestHash) { this.requestHash = requestHash; }
}
