package com.tcc.pjb.backend.core.processo.producao.domain;

import java.util.List;

public record ProcessoOperacaoControle(
        String codigo,
        String titulo,
        String estado,
        double cobertura,
        List<String> evidencias,
        List<String> proximasAcoes
) {
    public ProcessoOperacaoControle {
        codigo = codigo == null || codigo.isBlank() ? "CONTROLE" : codigo;
        titulo = titulo == null || titulo.isBlank() ? codigo : titulo;
        estado = estado == null || estado.isBlank() ? "PROVISIONADO" : estado;
        cobertura = Math.max(0d, Math.min(100d, cobertura));
        evidencias = evidencias == null ? List.of() : List.copyOf(evidencias);
        proximasAcoes = proximasAcoes == null ? List.of() : List.copyOf(proximasAcoes);
    }
}
