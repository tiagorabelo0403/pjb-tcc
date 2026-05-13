package com.tcc.pjb.backend.core.comunicacao.institucional.access.domain;

import java.util.List;

public record InstitutionalTextClosureItem(
        String code,
        String eixo,
        boolean implemented,
        List<String> evidences,
        List<String> fundamentos
) {
    public InstitutionalTextClosureItem {
        evidences = evidences == null ? List.of() : List.copyOf(evidences);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
