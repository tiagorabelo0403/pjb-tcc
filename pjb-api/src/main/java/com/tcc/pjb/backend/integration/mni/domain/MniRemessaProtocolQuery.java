package com.tcc.pjb.backend.integration.mni.domain;

public record MniRemessaProtocolQuery(
        Long processoId,
        String tribunalDestino,
        String motivo
) {}
