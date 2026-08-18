package com.tcc.pjb.backend.modules.notificacoes.api;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record NotificacaoPrazoCommand(
        Long usuarioId,
        Long processoId,
        String processoNumero,
        LocalDate vencimentoForense,
        LocalDateTime notificarEm,
        String titulo,
        String corpo,
        String urlDetalhes,
        String prioridade,
        String origemModulo,
        String notificationKey) {
}
