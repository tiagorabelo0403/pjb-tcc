package com.tcc.pjb.backend.core.processo.execucao.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoExecucaoAggregate(
        ProcessoExecucaoIdentity identity,
        boolean processoExecutivo,
        long totalTrilhas,
        long totalBloqueantes,
        long totalMandados,
        long totalOperacoesCustodia,
        List<ProcessoExecucaoTrilha> trilhas,
        List<String> alertas,
        List<String> proximoMelhorPasso,
        Instant geradoEm
) {
    public ProcessoExecucaoAggregate {
        Objects.requireNonNull(identity);
        trilhas = trilhas == null ? List.of() : List.copyOf(trilhas);
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        proximoMelhorPasso = proximoMelhorPasso == null ? List.of() : List.copyOf(proximoMelhorPasso);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
