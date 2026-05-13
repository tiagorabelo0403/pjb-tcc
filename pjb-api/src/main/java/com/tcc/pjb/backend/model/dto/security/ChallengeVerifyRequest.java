package com.tcc.pjb.backend.model.dto.security;

import jakarta.validation.constraints.NotBlank;

public class ChallengeVerifyRequest {

    @NotBlank
    private String code;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
