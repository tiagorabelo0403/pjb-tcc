package com.tcc.pjb.backend.core.processo.migracao.domain;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbFechamentoStatus;
import java.util.List;
import java.util.Objects;

public record ProcessoMigracaoFabricaItem(
        String codigo,
        String titulo,
        PjbFechamentoStatus status,
        boolean obrigatorio,
        int score,
        String origem,
        String destino,
        String estrategia,
        List<String> fundamentos
) {
    public ProcessoMigracaoFabricaItem {
        codigo = Objects.toString(codigo, "").trim();
        titulo = Objects.toString(titulo, "").trim();
        status = status == null ? PjbFechamentoStatus.PENDENTE : status;
        score = Math.max(0, Math.min(100, score));
        origem = Objects.toString(origem, "").trim();
        destino = Objects.toString(destino, "").trim();
        estrategia = Objects.toString(estrategia, "").trim();
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
