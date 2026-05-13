package com.tcc.pjb.backend.core.processo.unificado.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoMalhaObservabilidadeAggregate(
        Long processoId,
        String numeroProcesso,
        String saudeInstitucional,
        String saudeProcessual,
        int scoreRiscoMalha,
        long workItemsPendentesNacionais,
        long workItemsVencidosNacionais,
        List<String> filasCriticas,
        List<String> alertas,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoMalhaObservabilidadeAggregate {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        saudeInstitucional = Objects.toString(saudeInstitucional, "DESCONHECIDA").trim();
        saudeProcessual = Objects.toString(saudeProcessual, "ESTAVEL").trim();
        scoreRiscoMalha = Math.max(0, Math.min(100, scoreRiscoMalha));
        filasCriticas = filasCriticas == null ? List.of() : List.copyOf(filasCriticas);
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
