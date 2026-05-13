package com.tcc.pjb.backend.core.processo.sinalizacao.domain;

import java.time.Instant;
import java.util.List;

public record ProcessoSinalizacaoAggregate(
        Long processoId,
        String numeroProcesso,
        String accentColor,
        String highlightColor,
        String priorityBand,
        List<ProcessoSinalizacaoSeparador> separadores,
        List<String> fundamentos,
        List<String> alertas,
        Instant geradoEm
) {
    public ProcessoSinalizacaoAggregate {
        numeroProcesso = numeroProcesso == null ? "" : numeroProcesso;
        accentColor = accentColor == null || accentColor.isBlank() ? "slate" : accentColor;
        highlightColor = highlightColor == null || highlightColor.isBlank() ? accentColor : highlightColor;
        priorityBand = priorityBand == null || priorityBand.isBlank() ? "NORMAL" : priorityBand;
        separadores = separadores == null ? List.of() : List.copyOf(separadores);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
