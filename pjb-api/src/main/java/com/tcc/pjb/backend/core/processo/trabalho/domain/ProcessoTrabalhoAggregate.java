package com.tcc.pjb.backend.core.processo.trabalho.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoTrabalhoAggregate(
        ProcessoTrabalhoIdentity identity,
        long totalWorkItems,
        long pendentes,
        long emExecucao,
        long bloqueantes,
        long vencidos,
        long semResponsavelNominal,
        String faixaOperacional,
        List<ProcessoTrabalhoFila> filas,
        List<String> gates,
        List<String> proximoMelhorFluxo,
        Instant generatedAt
) {
    public ProcessoTrabalhoAggregate {
        Objects.requireNonNull(identity);
        Objects.requireNonNull(faixaOperacional);
        Objects.requireNonNull(generatedAt);
        filas = filas == null ? List.of() : List.copyOf(filas);
        gates = gates == null ? List.of() : List.copyOf(gates);
        proximoMelhorFluxo = proximoMelhorFluxo == null ? List.of() : List.copyOf(proximoMelhorFluxo);
    }
}
