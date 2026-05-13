package com.tcc.pjb.backend.model.dto.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class PasskeyStartRequest {

    @NotBlank
    @Email
    private String email;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
