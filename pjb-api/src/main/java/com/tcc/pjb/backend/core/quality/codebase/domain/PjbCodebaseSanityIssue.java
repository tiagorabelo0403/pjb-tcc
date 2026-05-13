package com.tcc.pjb.backend.core.quality.codebase.domain;

import java.util.List;
import java.util.Objects;

public record PjbCodebaseSanityIssue(
        String codigo,
        String severidade,
        String arquivo,
        List<Integer> linhas,
        String detalhe
) {
    public PjbCodebaseSanityIssue {
        codigo = Objects.toString(codigo, "").trim();
        severidade = Objects.toString(severidade, "INFO").trim();
        arquivo = Objects.toString(arquivo, "").trim();
        linhas = linhas == null ? List.of() : List.copyOf(linhas);
        detalhe = Objects.toString(detalhe, "").trim();
    }
}
