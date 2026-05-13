package com.tcc.pjb.backend.core.processo.orfandade.domain;

import java.time.Instant;
import java.util.List;

public record ProcessoAntiOrfaoAggregate(
        Long processoId,
        String numeroProcesso,
        long coberturaPercentual,
        long totalContextos,
        long totalConectados,
        long totalGaps,
        List<ProcessoAntiOrfaoCoverage> coberturas,
        List<ProcessoAntiOrfaoGap> gaps,
        List<String> proximasAcoes,
        Instant geradoEm
) {
    public ProcessoAntiOrfaoAggregate {
        numeroProcesso = numeroProcesso == null ? "" : numeroProcesso;
        coberturaPercentual = Math.max(0L, Math.min(100L, coberturaPercentual));
        totalContextos = Math.max(0L, totalContextos);
        totalConectados = Math.max(0L, totalConectados);
        totalGaps = Math.max(0L, totalGaps);
        coberturas = coberturas == null ? List.of() : List.copyOf(coberturas);
        gaps = gaps == null ? List.of() : List.copyOf(gaps);
        proximasAcoes = proximasAcoes == null ? List.of() : List.copyOf(proximasAcoes);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
