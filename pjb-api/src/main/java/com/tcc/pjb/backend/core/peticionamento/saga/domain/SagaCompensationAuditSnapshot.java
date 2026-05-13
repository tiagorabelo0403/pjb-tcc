package com.tcc.pjb.backend.core.peticionamento.saga.domain;

public record SagaCompensationAuditSnapshot(Long rascunhoId,
                                            String statusFinal,
                                            boolean compensated) {}
