package com.tcc.pjb.backend.integration.mni.domain;

public record MniProtocolView(
        Long remessaId,
        String protocoloDestino,
        String status
) {}
