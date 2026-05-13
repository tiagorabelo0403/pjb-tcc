package com.tcc.pjb.backend.core.peticionamento.saga.domain;

public record SagaAuditResult(
        SagaCompensationAuditSnapshot compensation,
        SagaNotificacaoPartesSnapshot notificacao,
        SagaTriagemSnapshot triagem
) {}
