package com.tcc.pjb.backend.modules.notificacoes.api;

public record NotificacaoPrazoDispatchResult(
        boolean aceita,
        String status,
        String notificationKey,
        String prioridade) {
}
