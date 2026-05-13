package com.tcc.pjb.backend.model.dto.security;

public record SigiloZkChallengeRequest(
        String escopo,
        String statement
) {
}
