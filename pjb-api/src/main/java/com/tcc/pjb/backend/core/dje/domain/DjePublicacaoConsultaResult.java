package com.tcc.pjb.backend.core.dje.domain;

public record DjePublicacaoConsultaResult(DjePublicacaoSnapshot publicacao,
                                          DjeNotificacaoSnapshot notificacao,
                                          DjeLifecycleExecutionSummary lifecycle) {
}
