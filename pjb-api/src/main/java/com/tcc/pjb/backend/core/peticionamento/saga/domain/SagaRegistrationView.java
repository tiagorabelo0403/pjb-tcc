package com.tcc.pjb.backend.core.peticionamento.saga.domain;

public record SagaRegistrationView(
        Long rascunhoId,
        boolean registrado,
        String processoRef
) {}
