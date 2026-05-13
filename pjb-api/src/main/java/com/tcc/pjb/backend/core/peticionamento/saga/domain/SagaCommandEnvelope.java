package com.tcc.pjb.backend.core.peticionamento.saga.domain;

public record SagaCommandEnvelope(
        Long rascunhoId,
        String etapa,
        String payloadHash
) {}
