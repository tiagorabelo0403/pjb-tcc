package com.tcc.pjb.backend.integration.mni.domain;

public record MniPayloadHashView(
        Long remessaId,
        String payloadHash,
        String motivo
) {}
