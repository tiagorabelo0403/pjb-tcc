package com.tcc.pjb.backend.core.peticionamento.saga.domain;

public record SagaProtocolView(
        Long rascunhoId,
        String numeroProtocolo,
        boolean ok
) {}
