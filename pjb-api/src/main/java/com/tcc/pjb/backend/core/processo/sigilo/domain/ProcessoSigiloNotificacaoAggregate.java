package com.tcc.pjb.backend.core.processo.sigilo.domain;

import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoIdentity;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoSigiloNotificacaoAggregate(
        ProcessoUnificadoIdentity identity,
        String statusPlanejamento,
        long totalDestinatarios,
        long totalComUsuario,
        long totalAltaPrioridade,
        List<String> channels,
        List<ProcessoSigiloNotificacaoItem> notificacoes,
        List<String> fundamentos,
        Instant generatedAt
) {
    public ProcessoSigiloNotificacaoAggregate {
        Objects.requireNonNull(identity);
        statusPlanejamento = statusPlanejamento == null || statusPlanejamento.isBlank() ? "SEM_ACAO" : statusPlanejamento;
        channels = channels == null ? List.of() : List.copyOf(channels);
        notificacoes = notificacoes == null ? List.of() : List.copyOf(notificacoes);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
    }
}
