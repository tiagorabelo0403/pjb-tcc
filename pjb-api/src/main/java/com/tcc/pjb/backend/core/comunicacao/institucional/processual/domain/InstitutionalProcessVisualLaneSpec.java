package com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain;

import java.util.List;
import java.util.Objects;

public record InstitutionalProcessVisualLaneSpec(
        String code,
        String title,
        String accentColor,
        int ordem,
        boolean active,
        List<String> filtros,
        List<String> etiquetas,
        List<String> fundamentos
) {
    public InstitutionalProcessVisualLaneSpec {
        Objects.requireNonNull(code);
        Objects.requireNonNull(title);
        Objects.requireNonNull(accentColor);
        filtros = filtros == null ? List.of() : List.copyOf(filtros);
        etiquetas = etiquetas == null ? List.of() : List.copyOf(etiquetas);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
