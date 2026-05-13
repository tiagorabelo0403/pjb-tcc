package com.tcc.pjb.backend.core.peticionamento.saga.domain;

public record SagaExecutionAuditView(Long rascunhoId, SagaExecutionTimeline timeline, SagaAuditResult audit) {}
