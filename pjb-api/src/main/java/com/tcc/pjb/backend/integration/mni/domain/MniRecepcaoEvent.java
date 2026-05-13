package com.tcc.pjb.backend.integration.mni.domain;

import java.time.Instant;

public record MniRecepcaoEvent(
        String tribunalOrigem,
        String numeroUnificado,
        String motivo,
        String payloadHash,
        Instant receivedAt,
        Long processoIdLocal,
        String status
) {
}
