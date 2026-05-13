package com.tcc.pjb.backend.core.peticionamento.saga.domain;

import java.time.Instant;

public record SagaExecutionStep(String etapa,
                                boolean success,
                                Instant executedAt) {}
