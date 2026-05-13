package com.tcc.pjb.backend.model.dto.julgamento.safety;

public record FaceStepUpIssueResponse(
        String token,
        long expiresAtEpoch,
        long ttlSeconds
) {}
