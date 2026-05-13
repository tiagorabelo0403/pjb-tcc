package com.tcc.pjb.backend.core.peticionamento.saga.domain;

public record SagaExecutionHealthSnapshot(Long rascunhoId, String status, boolean compensado) {}
