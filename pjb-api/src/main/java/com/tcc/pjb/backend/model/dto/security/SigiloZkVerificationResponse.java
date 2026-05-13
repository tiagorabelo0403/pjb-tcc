package com.tcc.pjb.backend.model.dto.security;

import java.time.Instant;

public record SigiloZkVerificationResponse(
        String challengeId,
        Long processoId,
        String numeroProcesso,
        boolean verificado,
        boolean snapshotHashConfere,
        boolean provaConfere,
        String status,
        Instant verificadoEm
) {
}
