package com.tcc.pjb.backend.core.peticionamento.saga.domain;

public record SagaStepResult(SagaStepView step, SagaCommandEnvelope envelope) {}
