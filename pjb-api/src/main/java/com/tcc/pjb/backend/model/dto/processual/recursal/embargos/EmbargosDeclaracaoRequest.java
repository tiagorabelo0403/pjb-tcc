package com.tcc.pjb.backend.model.dto.processual.recursal.embargos;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record EmbargosDeclaracaoRequest(
        Set<String> fundamentos,
        boolean efeitosInfringentesPretendidos,
        boolean contraDecisaoMonocratica,
        boolean interrompePrazoRecursalPrincipal,
        String observacoes) {

    public EmbargosDeclaracaoRequest {
        Objects.requireNonNull(fundamentos, "fundamentos");
        fundamentos = Set.copyOf(new LinkedHashSet<>(fundamentos));
        if (fundamentos.isEmpty()) {
            throw new IllegalArgumentException("fundamentos é obrigatório");
        }
        observacoes = observacoes == null ? null : normalize(observacoes);
    }

    private static String normalize(String value) {
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
