package com.tcc.pjb.backend.core.peticionamento.saga.domain;

public record SagaValidationResult(
        Long rascunhoId,
        boolean ok,
        int erros,
        String summary
) {}
