package com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain;

import java.util.List;
import java.util.Objects;

public record InstitutionalProcessSeparatorSpec(
        String code,
        String title,
        String accentColor,
        int ordem,
        boolean active,
        List<String> filtros,
        List<String> marcadores,
        List<String> fundamentos
) {
    public InstitutionalProcessSeparatorSpec {
        Objects.requireNonNull(code);
        Objects.requireNonNull(title);
        Objects.requireNonNull(accentColor);
        filtros = filtros == null ? List.of() : List.copyOf(filtros);
        marcadores = marcadores == null ? List.of() : List.copyOf(marcadores);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
