package com.tcc.pjb.backend.core.processo.painel.domain;

import java.time.Instant;
import java.util.List;

public record ProcessoPainelPrevidenciarioTrilhoAggregate(
        Long processoId,
        String numeroProcesso,
        boolean aplicavel,
        String statusGeral,
        String recomendacaoCnis,
        String filaPericialStatus,
        String pagamentoStatus,
        List<ProcessoPainelPrevidenciarioFonte> fontes,
        List<String> alertas,
        List<String> proximosPassos,
        Instant geradoEm
) {
    public ProcessoPainelPrevidenciarioTrilhoAggregate {
        numeroProcesso = numeroProcesso == null ? "" : numeroProcesso;
        statusGeral = statusGeral == null ? "NAO_APLICAVEL" : statusGeral;
        recomendacaoCnis = recomendacaoCnis == null ? "CNIS não analisado" : recomendacaoCnis;
        filaPericialStatus = filaPericialStatus == null ? "NAO_APLICAVEL" : filaPericialStatus;
        pagamentoStatus = pagamentoStatus == null ? "NAO_APLICAVEL" : pagamentoStatus;
        fontes = fontes == null ? List.of() : List.copyOf(fontes);
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        proximosPassos = proximosPassos == null ? List.of() : List.copyOf(proximosPassos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
