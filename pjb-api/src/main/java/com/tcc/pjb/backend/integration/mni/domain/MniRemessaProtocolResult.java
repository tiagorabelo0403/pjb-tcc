package com.tcc.pjb.backend.integration.mni.domain;

public record MniRemessaProtocolResult(
        Long processoId,
        String tribunalDestino,
        String protocoloDestino,
        String status,
        int tentativas
) {}
