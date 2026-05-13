package com.tcc.pjb.backend.core.comunicacao.institucional.access.domain;

import java.util.List;

public record InstitutionalOnboardingStep(
        String stepCode,
        String title,
        String owner,
        boolean blocking,
        List<String> requiredArtifacts,
        List<String> fundamentos
) {
    public InstitutionalOnboardingStep {
        requiredArtifacts = requiredArtifacts == null ? List.of() : List.copyOf(requiredArtifacts);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
