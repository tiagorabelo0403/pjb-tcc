package com.tcc.pjb.backend.core.peticionamento.saga.domain;
public record SagaWorkerExecutionSnapshot(Long rascunhoId, String etapa, boolean success) {}
