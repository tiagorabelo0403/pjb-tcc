package com.tcc.pjb.backend.core.peticionamento.saga.domain;

public record SagaProtocolQuery(
        Long rascunhoId,
        String numeroProtocolo
) {}
