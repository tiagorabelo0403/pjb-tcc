package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

import java.util.List;
import java.util.Objects;

public record PjbArquiteturaSubstituicaoCapacidade(
        String codigo,
        String titulo,
        PjbFechamentoStatus status,
        int score,
        String conclusao,
        List<String> evidencias,
        List<String> pendencias
) {
    public PjbArquiteturaSubstituicaoCapacidade {
        codigo = Objects.toString(codigo, "").trim();
        titulo = Objects.toString(titulo, "").trim();
        status = status == null ? PjbFechamentoStatus.PENDENTE : status;
        score = Math.max(0, Math.min(100, score));
        conclusao = Objects.toString(conclusao, "").trim();
        evidencias = evidencias == null ? List.of() : List.copyOf(evidencias);
        pendencias = pendencias == null ? List.of() : List.copyOf(pendencias);
    }

    public boolean concluida() {
        return status == PjbFechamentoStatus.CONCLUIDA;
    }
}
