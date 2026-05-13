package com.tcc.pjb.backend.core.peticionamento.saga.domain;

import java.time.Instant;

public record SagaCommandAuditSnapshot(
        Long rascunhoId,
        String etapa,
        Instant executadoEm
) {}
