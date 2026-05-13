package com.tcc.pjb.backend.integration.mni.domain;

public record MniRecepcaoResult(
        String tribunalOrigem,
        String numeroUnificado,
        String motivo,
        String payloadHash,
        Long processoIdLocal,
        String status
) {
}
