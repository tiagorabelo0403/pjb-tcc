package com.tcc.pjb.backend.core.peticionamento.saga.domain;

public record SagaHealthResult(
        Long rascunhoId,
        String status,
        boolean compensado
) {}
