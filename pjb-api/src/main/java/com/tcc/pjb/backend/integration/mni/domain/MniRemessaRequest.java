package com.tcc.pjb.backend.integration.mni.domain;

public record MniRemessaRequest(
        Long processoId,
        String tribunalDestino,
        String motivo
) {
}
