package com.tcc.pjb.backend.model.dto.security;

public record SigiloZkVerificationRequest(
        String snapshotHashConhecido,
        String proofToken
) {
}
