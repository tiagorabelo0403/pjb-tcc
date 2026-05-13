package com.tcc.pjb.backend.core.governance.institucional.domain;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbFechamentoStatus;
import java.util.List;
import java.util.Objects;

public record PjbGovernancaInstitucionalMarco(
        String codigo,
        String titulo,
        PjbFechamentoStatus status,
        int score,
        String trilhaInstitucional,
        String proximaDeliberacao,
        List<String> fundamentos
) {
    public PjbGovernancaInstitucionalMarco {
        codigo = Objects.toString(codigo, "").trim();
        titulo = Objects.toString(titulo, "").trim();
        status = status == null ? PjbFechamentoStatus.PENDENTE : status;
        score = Math.max(0, Math.min(100, score));
        trilhaInstitucional = Objects.toString(trilhaInstitucional, "").trim();
        proximaDeliberacao = Objects.toString(proximaDeliberacao, "").trim();
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
