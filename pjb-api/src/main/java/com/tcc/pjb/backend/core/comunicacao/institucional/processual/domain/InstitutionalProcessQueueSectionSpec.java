package com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain;

import java.util.List;
import java.util.Objects;

public record InstitutionalProcessQueueSectionSpec(
        String code,
        String title,
        String accentColor,
        int ordem,
        List<String> filtros,
        List<String> indicadores,
        List<String> ordenacoes
) {
    public InstitutionalProcessQueueSectionSpec {
        Objects.requireNonNull(code);
        Objects.requireNonNull(title);
        Objects.requireNonNull(accentColor);
        filtros = filtros == null ? List.of() : List.copyOf(filtros);
        indicadores = indicadores == null ? List.of() : List.copyOf(indicadores);
        ordenacoes = ordenacoes == null ? List.of() : List.copyOf(ordenacoes);
    }
}
