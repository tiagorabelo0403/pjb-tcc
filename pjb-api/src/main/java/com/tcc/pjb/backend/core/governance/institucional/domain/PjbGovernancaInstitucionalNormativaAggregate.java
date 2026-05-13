package com.tcc.pjb.backend.core.governance.institucional.domain;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbFechamentoStatus;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record PjbGovernancaInstitucionalNormativaAggregate(
        Long processoId,
        String numeroProcesso,
        List<PjbGovernancaInstitucionalMarco> marcos,
        int scoreGeral,
        PjbFechamentoStatus statusGeral,
        boolean prontoGovernanca,
        List<String> pendencias,
        List<String> fundamentos,
        Instant geradoEm
) {
    public PjbGovernancaInstitucionalNormativaAggregate {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        marcos = marcos == null ? List.of() : List.copyOf(marcos);
        scoreGeral = Math.max(0, Math.min(100, scoreGeral));
        statusGeral = statusGeral == null ? PjbFechamentoStatus.PENDENTE : statusGeral;
        pendencias = pendencias == null ? List.of() : List.copyOf(pendencias);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
