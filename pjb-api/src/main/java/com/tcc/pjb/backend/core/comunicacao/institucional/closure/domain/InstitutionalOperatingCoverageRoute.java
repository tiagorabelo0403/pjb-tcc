package com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain;

import java.util.List;

public record InstitutionalOperatingCoverageRoute(
        String requestedMunicipality,
        String requestedUf,
        String requestedKind,
        boolean localUnitPresent,
        String responsibleUnitCode,
        String responsibleUnitName,
        String responsibleForo,
        String responsibleComarca,
        String responsibleTribunalCode,
        String coverageMode,
        List<String> fallbackChain,
        List<String> fundamentos
) {
    public InstitutionalOperatingCoverageRoute {
        fallbackChain = fallbackChain == null ? List.of() : List.copyOf(fallbackChain);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }

    public boolean localCoveragePresent() {
        return localUnitPresent;
    }
}
