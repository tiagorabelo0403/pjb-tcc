package com.tcc.pjb.backend.core.notificacao;

import java.util.Objects;

public record NotificacaoEnvelope(
        String destinatarioCpfCnpj,
        String destinatarioNome,
        CanalNotificacao canal,
        String assunto,
        String corpo,
        PrioridadeNotificacao prioridade,
        String processoId,
        String origemModulo
) {
    public NotificacaoEnvelope {
        Objects.requireNonNull(destinatarioCpfCnpj, "destinatarioCpfCnpj");
        Objects.requireNonNull(canal, "canal");
        Objects.requireNonNull(corpo, "corpo");
        prioridade = prioridade == null ? PrioridadeNotificacao.NORMAL : prioridade;
    }
}
