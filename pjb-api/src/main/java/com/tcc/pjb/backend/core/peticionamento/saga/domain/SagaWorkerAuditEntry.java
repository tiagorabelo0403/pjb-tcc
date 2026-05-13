package com.tcc.pjb.backend.core.peticionamento.saga.domain;
import java.time.Instant;
public record SagaWorkerAuditEntry(Long rascunhoId, String etapa, Instant executedAt) {}
