package com.tcc.pjb.backend.core.peticionamento.saga.domain;

import java.util.List;

public record SagaExecutionTimeline(Long rascunhoId,
                                    List<SagaExecutionStep> steps) {}
