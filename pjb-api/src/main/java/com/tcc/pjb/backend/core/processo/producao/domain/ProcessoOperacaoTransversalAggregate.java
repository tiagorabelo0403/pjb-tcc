package com.tcc.pjb.backend.core.processo.producao.domain;

import java.time.Instant;
import java.util.List;

public record ProcessoOperacaoTransversalAggregate(
        Long processoId,
        String numeroProcesso,
        String readiness,
        double coberturaGlobal,
        double saturacao,
        List<ProcessoOperacaoControle> controles,
        List<String> alertas,
        List<String> proximasAcoes,
        Instant geradoEm
) {
    public ProcessoOperacaoTransversalAggregate {
        numeroProcesso = numeroProcesso == null ? "" : numeroProcesso;
        readiness = readiness == null || readiness.isBlank() ? "NOT_READY" : readiness;
        coberturaGlobal = Math.max(0d, Math.min(100d, coberturaGlobal));
        saturacao = Math.max(0d, Math.min(100d, saturacao));
        controles = controles == null ? List.of() : List.copyOf(controles);
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        proximasAcoes = proximasAcoes == null ? List.of() : List.copyOf(proximasAcoes);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
