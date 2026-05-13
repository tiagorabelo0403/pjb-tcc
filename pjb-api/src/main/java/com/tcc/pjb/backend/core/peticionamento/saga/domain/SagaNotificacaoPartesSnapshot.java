package com.tcc.pjb.backend.core.peticionamento.saga.domain;

public record SagaNotificacaoPartesSnapshot(Long rascunhoId,
                                            boolean notified,
                                            String status) {}
