package com.tcc.pjb.backend.core.peticionamento.saga.domain;

public record SagaCompensationHealthView(
        Long rascunhoId,
        boolean compensado,
        String reason
) {}
