package com.tcc.pjb.backend.core.peticionamento.saga.domain;

public record SagaCompensationQuery(
        Long rascunhoId,
        String reason
) {}
