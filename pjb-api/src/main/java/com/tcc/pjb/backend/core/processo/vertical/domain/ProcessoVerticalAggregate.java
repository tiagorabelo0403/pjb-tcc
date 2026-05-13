package com.tcc.pjb.backend.core.processo.vertical.domain;

import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoIdentity;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoVerticalAggregate(
        String sliceCode,
        String sliceTitle,
        ProcessoUnificadoIdentity identity,
        String ritoDominante,
        String faseAtual,
        String statusAtual,
        long totalEtapas,
        long totalLanes,
        long totalPendenciasCriticas,
        long totalHandoffs,
        List<ProcessoVerticalLane> lanes,
        List<ProcessoVerticalEtapa> etapas,
        List<String> processChips,
        List<String> nextBestFlow,
        List<String> alertas,
        List<String> fundamentos,
        Instant generatedAt
) {
    public ProcessoVerticalAggregate {
        Objects.requireNonNull(sliceCode);
        Objects.requireNonNull(sliceTitle);
        Objects.requireNonNull(identity);
        ritoDominante = ritoDominante == null ? "NAO_INFORMADO" : ritoDominante;
        faseAtual = faseAtual == null ? "NAO_INFORMADO" : faseAtual;
        statusAtual = statusAtual == null ? "NAO_INFORMADO" : statusAtual;
        lanes = lanes == null ? List.of() : List.copyOf(lanes);
        etapas = etapas == null ? List.of() : List.copyOf(etapas);
        processChips = processChips == null ? List.of() : List.copyOf(processChips);
        nextBestFlow = nextBestFlow == null ? List.of() : List.copyOf(nextBestFlow);
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
    }
}
