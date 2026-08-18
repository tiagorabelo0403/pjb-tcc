package com.tcc.pjb.backend.modules.notificacoes.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record NotificacaoPrazoNormalizada(
        Long usuarioId,
        Long processoId,
        String processoNumero,
        LocalDate vencimentoForense,
        LocalDateTime notificarEm,
        String titulo,
        String corpo,
        String urlDetalhes,
        NotificacaoPrazoPrioridade prioridade,
        String origemModulo,
        String notificationKey) {
}
