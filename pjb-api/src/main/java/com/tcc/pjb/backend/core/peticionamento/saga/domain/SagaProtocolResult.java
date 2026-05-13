package com.tcc.pjb.backend.core.peticionamento.saga.domain;

public record SagaProtocolResult(
        Long rascunhoId,
        String numeroProtocolo,
        String status,
        boolean generated
) {}
