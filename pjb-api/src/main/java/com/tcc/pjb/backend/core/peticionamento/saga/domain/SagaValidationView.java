package com.tcc.pjb.backend.core.peticionamento.saga.domain;

import java.util.List;

public record SagaValidationView(
        Long rascunhoId,
        boolean ok,
        List<String> erros
) {}
