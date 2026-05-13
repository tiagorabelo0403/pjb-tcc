package com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalDelegatedGovernanceClosure(
        String scopeFilter,
        List<String> perfisDiretosPermitidos,
        List<InstitutionalDelegatedScopeCoverage> escoposDelegados,
        List<InstitutionalDelegatedGovernanceItem> itens,
        List<String> fundamentos,
        Instant generatedAt
) {
    public InstitutionalDelegatedGovernanceClosure {
        perfisDiretosPermitidos = perfisDiretosPermitidos == null ? List.of() : List.copyOf(perfisDiretosPermitidos);
        escoposDelegados = escoposDelegados == null ? List.of() : List.copyOf(escoposDelegados);
        itens = itens == null ? List.of() : List.copyOf(itens);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
